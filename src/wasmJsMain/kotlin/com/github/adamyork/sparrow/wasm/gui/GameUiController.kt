package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.ui.graphics.Color
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
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.ScoreService
import com.github.adamyork.sparrow.wasm.service.WavService
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset
import com.github.adamyork.sparrow.wasm.service.data.LoadingTask
import com.github.adamyork.sparrow.wasm.service.data.TextAsset
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

    var isInitialized: Boolean = false

    val loadingTasks: List<LoadingTask>
        get() = viewModel.loadingTasks

    override fun onTaskCompleted(taskId: String) {
        logger.info { "task id is $taskId" }
        logger.info { "mapped key task is ${LoadingViewModel.mapKeyToTaskId(taskId)}" }
        viewModel.onTaskCompleted(LoadingViewModel.mapKeyToTaskId(taskId))
    }

    suspend fun initializeGame(): ImageBitmap? {
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
                        val value = loader() // Now safe to call suspend function
                        viewModel.onTaskCompleted(LoadingViewModel.mapKeyToTaskId(key))
                        key to value
                    }
                }.awaitAll().toMap()
            }
            val viewPort = ViewPort(
                assetService.gameConfig.viewport.x,
                assetService.gameConfig.viewport.y,
                0, 0,
                assetService.gameConfig.viewport.width,
                assetService.gameConfig.viewport.height
            )
            val gameMap = loadedAssets["map"] as GameMap
            gameStateElements = GameStateElements(
                viewPort = viewPort,
                player = engine.createDefaultPlayer(loadedAssets["player"] as ImageAsset),
                gameMap = gameMap,
                playerAsset = loadedAssets["player"] as ImageAsset,
                mapItemCollectibleAsset = loadedAssets["collectible item"] as ImageAsset,
                mapItemFinishAsset = loadedAssets["finish item"] as ImageAsset,
                mapEnemyBlockerAsset = loadedAssets["blocker enemy"] as ImageAsset,
                mapEnemyShooterAsset = loadedAssets["shooter enemy"] as ImageAsset
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
            engine.setCollisionBufferedImage(gameMap.collisionAsset)
            particles.populateColorMap(assetService)
            scoreService.gameMapItem = gameMap.items
            isInitialized = true
            logger.info { "splash loaded and game initialized" }
            loadedImage
        }.onFailure { logger.error { "init failed $it" } }.getOrNull()
    }

    fun allTasksCompleted(): Boolean {
        return loadingTasks.all { it.isCompleted }
    }

    fun start() {
        statusProvider.running = true
        wavService.playBackgroundAudio()
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
        stateElements.viewPort = ViewPort(
            assetService.gameConfig.viewport.x,
            assetService.gameConfig.viewport.y,
            0,
            0,
            assetService.gameConfig.viewport.width,
            assetService.gameConfig.viewport.height
        )
        scoreService.gameMapItem = stateElements.gameMap.items
        statusProvider.reset()
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

    fun tick(): GeneralUiState {
        val drawResult = next()
        wavService.playNext()
        val currentFps = statusProvider.getFps()
        val scoreLabels = getScoreLabels()
        val statusText = getStatusText()
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
        if (isInitialized) {
            when (controlType) {
                ControlType.START -> {
                    stateElements.player = engine.startInput(controlAction, stateElements.player)
                }

                ControlType.STOP -> {
                    stateElements.player = engine.stopInput(controlAction, stateElements.player)
                }
            }
        }
    }

    private fun getStatusText(): TextAsset {
        return if (isInitialized) {
            assetService.getTextForGameState(stateElements.gameMap.state)
        } else {
            TextAsset("Press Start To Begin", Color.Black)
        }
    }

    private fun next(): DrawResult {
        val now = statusProvider.getCurrentFrameTime()
        if (!statusProvider.running || !isInitialized || !statusProvider.atOrUnderFpsMax(now)) {
            return DrawResult.EMPTY_DRAW_RESULT
        }
        val collisionBoundaries = engine.getCollisionBoundaries(stateElements.player)
        stateElements.player = engine.managePlayer(stateElements.player, collisionBoundaries)
        stateElements.viewPort = engine.manageViewport(stateElements.player, stateElements.viewPort)
        stateElements.gameMap = engine.manageMap(stateElements.player, stateElements.gameMap, stateElements.viewPort)
        val (nextPlayer, nextMap) = engine.manageEnemyAndItemCollision(
            stateElements.player,
            stateElements.gameMap,
            stateElements.viewPort
        )
        stateElements.player = nextPlayer
        stateElements.gameMap = nextMap
        scoreService.gameMapItem = stateElements.gameMap.items
        return engine.draw(stateElements.gameMap, stateElements.viewPort, stateElements.player)
    }
}

