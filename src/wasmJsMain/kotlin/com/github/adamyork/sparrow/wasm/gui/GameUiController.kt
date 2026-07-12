package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.ui.graphics.ImageBitmap
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.common.data.ControlAction
import com.github.adamyork.sparrow.wasm.common.data.ControlType
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.engine.Engine
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.engine.data.DrawResult
import com.github.adamyork.sparrow.wasm.gui.data.GameStateElements
import com.github.adamyork.sparrow.wasm.gui.data.GeneralUiState
import com.github.adamyork.sparrow.wasm.gui.data.ScoreUiState
import com.github.adamyork.sparrow.wasm.gui.data.ScreenDimensions
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.ScoreService
import com.github.adamyork.sparrow.wasm.service.WavService
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset
import com.github.adamyork.sparrow.wasm.service.data.LoadingTask
import com.github.adamyork.sparrow.wasm.service.v1.LoadingProgressListener
import com.github.adamyork.sparrow.wasm.service.v1.LoadingViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class GameUiController(
    private val assetService: AssetService,
    private val engine: Engine,
    private val particles: Particles,
    private val scoreService: ScoreService,
    private val statusProvider: StatusProvider,
    private val wavService: WavService
) : LoadingProgressListener {

    private val logger = KotlinLogging.logger {}
    private val viewModel = LoadingViewModel()
    private var gameStateElements: GameStateElements? = null
    private val stateElements: GameStateElements
        get() = gameStateElements ?: throw IllegalStateException("Game not initialized")
    private var isInitialized: Boolean = false
    private var screenDimensions: ScreenDimensions? = null
    val loadingTasks: List<LoadingTask>
        get() = viewModel.loadingTasks

    suspend fun initializeGame(screenDimensions: ScreenDimensions): ImageBitmap? {
        return runCatching {
            logger.info { "initializing" }
            assetService.initialize(this)
            logger.info { "loading splash image" }
            val loadedImage = assetService.loadBufferedImageAsync("https://sparrow-assets.pages.dev/splash.png")
                .also { viewModel.onTaskCompleted("splash") }
            val loaders: Map<String, suspend () -> Any> = mapOf(
                "map" to { assetService.loadMap(0, this@GameUiController) },
                "player" to { assetService.loadPlayer() },
                "collectible item" to { assetService.loadItem(0) },
                "finish item" to { assetService.loadItem(1) },
                "blocker enemy" to { assetService.loadEnemy(0) },
                "shooter enemy" to { assetService.loadEnemy(1) },
                "game audio" to { assetService.loadAudio(this@GameUiController) }
            )
            val loadedAssets = coroutineScope {
                loaders.map { (key, loader) ->
                    async {
                        val value = loader()
                        viewModel.onTaskCompleted(LoadingViewModel.mapKeyToTaskId(key))
                        key to value
                    }
                }.awaitAll().toMap()
            }
            val viewPort = createInitialViewPort(screenDimensions)
            this.screenDimensions = screenDimensions
            val gameMap = loadedAssets.getValue("map") as GameMap
            val playerAsset = loadedAssets.getValue("player") as ImageAsset
            val collectibleAsset = loadedAssets.getValue("collectible item") as ImageAsset
            val finishAsset = loadedAssets.getValue("finish item") as ImageAsset
            val blockerAsset = loadedAssets.getValue("blocker enemy") as ImageAsset
            val shooterAsset = loadedAssets.getValue("shooter enemy") as ImageAsset
            val player = engine.createDefaultPlayer(playerAsset)
            gameStateElements = GameStateElements(
                viewPort = viewPort,
                player = player,
                gameMap = gameMap,
                playerAsset = playerAsset,
                mapItemCollectibleAsset = collectibleAsset,
                mapItemFinishAsset = finishAsset,
                mapEnemyBlockerAsset = blockerAsset,
                mapEnemyShooterAsset = shooterAsset
            )
            gameMap.generateMapItems(
                stateElements.mapItemCollectibleAsset,
                stateElements.mapItemFinishAsset,
                assetService
            )
            gameMap.generateMapEnemies(
                stateElements.mapEnemyBlockerAsset,
                stateElements.mapEnemyShooterAsset,
                assetService
            )
            engine.initialize(gameMap, gameMap.collisionAsset, player)
            particles.populateColorMap(assetService)
            scoreService.gameMapItem = gameMap.items
            isInitialized = true
            logger.info { "splash loaded and game initialized" }
            loadedImage
        }.onFailure { logger.error { "init failed $it" } }.getOrNull()
    }

    override fun onTaskCompleted(taskId: String) {
        val mappedTaskId = LoadingViewModel.mapKeyToTaskId(taskId)
        logger.info { "task id is $taskId" }
        logger.info { "mapped key task is $mappedTaskId" }
        viewModel.onTaskCompleted(mappedTaskId)
    }

    fun allTasksCompleted(): Boolean = loadingTasks.all { it.isCompleted }

    fun start() {
        statusProvider.running = true
        wavService.playBackgroundAudio()
    }

    fun getScoreLabels(): ScoreUiState {
        val total = scoreService.getTotal()
        val remaining = scoreService.getRemaining()
        val score = (total - remaining).coerceAtLeast(0)
        return ScoreUiState(
            scoreLabel = "Score: $score",
            totalLabel = "Total: $total",
            remainingLabel = "Remaining: $remaining"
        )
    }

    fun tick(timestamp: Double): GeneralUiState {
        val currentFps = statusProvider.getFps()
        val scoreLabels = getScoreLabels()
        val maybeGameState = if (isInitialized) stateElements.gameMap.state else null
        val statusText = assetService.getTextForGameState(maybeGameState)
        if (!statusProvider.running || !isInitialized) {
            return GeneralUiState(
                drawResult = DrawResult.EMPTY_DRAW_RESULT,
                fpsLabel = "FPS: ${currentFps.toInt()}",
                gameStatusLabel = statusText.message,
                gameStatusLabelColor = statusText.color,
                scoreLabel = scoreLabels.scoreLabel,
                totalLabel = scoreLabels.totalLabel,
                remainingLabel = scoreLabels.remainingLabel
            )
        }
        val collisionBoundaries = engine.getCollisionBoundaries(stateElements.player)
        engine.managePlayer(stateElements.player, collisionBoundaries)
        engine.manageViewport(stateElements.player, stateElements.viewPort)
        engine.manageMap(stateElements.player, stateElements.gameMap, stateElements.viewPort)
        engine.manageEnemyAndItemCollision(stateElements.player, stateElements.gameMap, stateElements.viewPort)

        scoreService.gameMapItem = stateElements.gameMap.items
        val drawResult = engine.draw(stateElements.gameMap, stateElements.viewPort, stateElements.player, timestamp)
        wavService.playNext()
        return GeneralUiState(
            drawResult = drawResult,
            fpsLabel = "FPS: ${currentFps.toInt()}",
            gameStatusLabel = statusText.message,
            gameStatusLabelColor = statusText.color,
            scoreLabel = scoreLabels.scoreLabel,
            totalLabel = scoreLabels.totalLabel,
            remainingLabel = scoreLabels.remainingLabel
        )
    }

    fun applyInput(controlType: ControlType, controlAction: ControlAction) {
        if (!isInitialized) return
        when (controlType) {
            ControlType.START -> {
                engine.startInput(controlAction, stateElements.player)
            }

            ControlType.STOP -> {
                engine.stopInput(controlAction, stateElements.player)
            }
        }
    }

    fun pause() {
        statusProvider.running = false
    }

    fun reset() {
        if (!isInitialized) {
            return
        }
        logger.info { "reset game" }
        stateElements.player = engine.createDefaultPlayer(stateElements.playerAsset)
        stateElements.gameMap.reset(
            stateElements.mapItemCollectibleAsset,
            stateElements.mapItemFinishAsset,
            stateElements.mapEnemyBlockerAsset,
            stateElements.mapEnemyShooterAsset,
            assetService
        )
        stateElements.viewPort = createInitialViewPort(screenDimensions!!)
        scoreService.gameMapItem = stateElements.gameMap.items
        statusProvider.reset()
    }

    private fun createInitialViewPort(screenDimensions: ScreenDimensions): ViewPort {
        return ViewPort(
            assetService.gameConfig.viewport.x,
            assetService.gameConfig.viewport.y,
            0,
            0,
            screenDimensions.width,
            screenDimensions.height
        )
    }
}

