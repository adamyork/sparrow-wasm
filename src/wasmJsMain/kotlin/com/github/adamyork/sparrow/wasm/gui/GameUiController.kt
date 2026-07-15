package com.github.adamyork.sparrow.wasm.gui

import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.common.data.ControlAction
import com.github.adamyork.sparrow.wasm.common.data.ControlType
import com.github.adamyork.sparrow.wasm.common.data.GameLifeCycleState
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.engine.Engine
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.engine.data.DrawResult
import com.github.adamyork.sparrow.wasm.gui.data.GameStateElements
import com.github.adamyork.sparrow.wasm.gui.data.GeneralUiState
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
    private val wavService: WavService,
    private val screenDimensionsService: ScreenDimensionsService
) : LoadingProgressListener {

    private val logger = KotlinLogging.logger {}
    private val viewModel = LoadingViewModel()

    val gameStateElements: GameStateElements = GameStateElements.emptyGameStateElements
    val loadingTasks: List<LoadingTask>
        get() = viewModel.loadingTasks

    suspend fun initializeGame() {
        runCatching {
            logger.info { "initializing" }
            val screenDimensions = screenDimensionsService.getScreenDimensions()
            assetService.initialize(this)
            val loaders: Map<String, suspend () -> Any> = mapOf(
                "splash" to { assetService.loadSplash() },
                "ending" to { assetService.loadEnding() },
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
            val splashImage = (loadedAssets.getValue("splash") as ImageAsset).imageAndBytes.imageBitmap
            val endingImage = (loadedAssets.getValue("ending") as ImageAsset).imageAndBytes.imageBitmap
            val viewPort = createInitialViewPort(screenDimensions)
            val gameMap = loadedAssets.getValue("map") as GameMap
            val playerAsset = loadedAssets.getValue("player") as ImageAsset
            val collectibleAsset = loadedAssets.getValue("collectible item") as ImageAsset
            val finishAsset = loadedAssets.getValue("finish item") as ImageAsset
            val blockerAsset = loadedAssets.getValue("blocker enemy") as ImageAsset
            val shooterAsset = loadedAssets.getValue("shooter enemy") as ImageAsset
            val player = engine.createDefaultPlayer(playerAsset)
            gameStateElements.viewPort = viewPort
            gameStateElements.player = player
            gameStateElements.gameMap = gameMap
            gameStateElements.splashImage = splashImage
            gameStateElements.endingImage = endingImage
            gameStateElements.playerAsset = playerAsset
            gameStateElements.mapItemCollectibleAsset = collectibleAsset
            gameStateElements.mapItemFinishAsset = finishAsset
            gameStateElements.mapEnemyBlockerAsset = blockerAsset
            gameStateElements.mapEnemyShooterAsset = shooterAsset
            gameStateElements.scoreLabel = "Score: --"
            gameStateElements.totalLabel = "Total: --"
            gameStateElements.remainingLabel = "Remaining: --"
            gameMap.generateMapItems(
                gameStateElements.mapItemCollectibleAsset,
                gameStateElements.mapItemFinishAsset,
                assetService
            )
            gameMap.generateMapEnemies(
                gameStateElements.mapEnemyBlockerAsset,
                gameStateElements.mapEnemyShooterAsset,
                assetService
            )
            engine.initialize(gameMap, gameMap.collisionAsset, player)
            particles.populateColorMap(assetService)
            scoreService.gameMapItem = gameMap.items
            refreshScoreLabels()
            statusProvider.gameMapState = gameMap.state
            statusProvider.gameLifeCycleState = GameLifeCycleState.INITIALIZED
            logger.info { "splash loaded and game initialized" }
        }.onFailure { logger.error { "init failed $it" } }
    }

    private fun refreshScoreLabels() {
        val total = scoreService.getTotal()
        val remaining = scoreService.getRemaining()
        val score = (total - remaining).coerceAtLeast(0)
        gameStateElements.scoreLabel = "Score: $score"
        gameStateElements.totalLabel = "Total: $total"
        gameStateElements.remainingLabel = "Remaining: $remaining"
    }

    override fun onTaskCompleted(taskId: String) {
        val mappedTaskId = LoadingViewModel.mapKeyToTaskId(taskId)
        logger.info { "task id is $taskId" }
        logger.info { "mapped key task is $mappedTaskId" }
        viewModel.onTaskCompleted(mappedTaskId)
    }

    fun allTasksCompleted(): Boolean = loadingTasks.all { it.isCompleted }

    fun start() {
        if (statusProvider.gameMapState == GameMapState.COMPLETED) {
            reset()
        }
        statusProvider.gameLifeCycleState = GameLifeCycleState.RUNNING
        wavService.playBackgroundAudio()
    }

    fun tick(timestamp: Double): GeneralUiState {
        val currentFps = statusProvider.getFps()
        val elements = gameStateElements
        if (statusProvider.gameLifeCycleState != GameLifeCycleState.INITIALIZING) {
            statusProvider.gameMapState = elements.gameMap.state
        }
        val statusText = assetService.getTextForGameState(statusProvider.gameMapState)
        if (statusProvider.gameLifeCycleState != GameLifeCycleState.RUNNING || statusProvider.gameLifeCycleState == GameLifeCycleState.INITIALIZING) {
            return GeneralUiState(
                drawResult = DrawResult.EMPTY_DRAW_RESULT,
                fpsLabel = "FPS: ${currentFps.toInt()}",
                gameStatusLabel = statusText.message,
                gameStatusLabelColor = statusText.color,
                scoreLabel = elements.scoreLabel,
                totalLabel = elements.totalLabel,
                remainingLabel = elements.remainingLabel,
                gameMapState = statusProvider.gameMapState,
                completionTransitionRequested = false
            )
        }
        val collisionBoundaries = engine.getCollisionBoundaries(elements.player)
        engine.managePlayer(elements.player, collisionBoundaries)
        engine.manageViewport(elements.player, elements.viewPort)
        engine.manageMap(elements.player, elements.gameMap, elements.viewPort)
        engine.manageEnemyAndItemCollision(elements.player, elements.gameMap, elements.viewPort)

        scoreService.gameMapItem = elements.gameMap.items
        refreshScoreLabels()
        val drawResult = engine.draw(elements.gameMap, elements.viewPort, elements.player, timestamp)
        wavService.playNext()
        val currentGameState = elements.gameMap.state
        statusProvider.gameMapState = currentGameState
        if (currentGameState == GameMapState.COMPLETED && statusProvider.gameLifeCycleState == GameLifeCycleState.RUNNING) {
            pause()
        }
        return GeneralUiState(
            drawResult = drawResult,
            fpsLabel = "FPS: ${currentFps.toInt()}",
            gameStatusLabel = statusText.message,
            gameStatusLabelColor = statusText.color,
            scoreLabel = elements.scoreLabel,
            totalLabel = elements.totalLabel,
            remainingLabel = elements.remainingLabel,
            gameMapState = currentGameState,
            completionTransitionRequested = currentGameState == GameMapState.COMPLETED
        )
    }

    fun applyInput(controlType: ControlType, controlAction: ControlAction) {
        val elements = gameStateElements
        if (statusProvider.gameLifeCycleState == GameLifeCycleState.INITIALIZING) return
        when (controlType) {
            ControlType.START -> {
                engine.startInput(controlAction, elements.player)
            }

            ControlType.STOP -> {
                engine.stopInput(controlAction, elements.player)
            }
        }
    }

    fun pause() {
        statusProvider.gameLifeCycleState = when {
            statusProvider.gameMapState == GameMapState.COMPLETED -> GameLifeCycleState.COMPLETED
            statusProvider.gameLifeCycleState != GameLifeCycleState.INITIALIZING -> GameLifeCycleState.PAUSED
            else -> GameLifeCycleState.INITIALIZING
        }
    }

    fun reset() {
        logger.info { "reset game" }
        gameStateElements.player = engine.createDefaultPlayer(gameStateElements.playerAsset)
        gameStateElements.gameMap.reset(
            gameStateElements.mapItemCollectibleAsset,
            gameStateElements.mapItemFinishAsset,
            gameStateElements.mapEnemyBlockerAsset,
            gameStateElements.mapEnemyShooterAsset,
            assetService
        )
        gameStateElements.viewPort = createInitialViewPort(screenDimensionsService.getScreenDimensions())
        scoreService.gameMapItem = gameStateElements.gameMap.items
        refreshScoreLabels()
        statusProvider.reset()
        statusProvider.gameMapState = gameStateElements.gameMap.state
        statusProvider.gameLifeCycleState = GameLifeCycleState.RUNNING
    }

    private fun createInitialViewPort(screenDimensions: ScreenDimensions): ViewPort {
        return ViewPort(
            assetService.appProperties.viewport.x,
            assetService.appProperties.viewport.y,
            0,
            0,
            screenDimensions.width,
            screenDimensions.height
        )
    }
}
