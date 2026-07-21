package com.github.adamyork.sparrow.platform.gui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.github.adamyork.sparrow.platform.common.data.ControlAction
import com.github.adamyork.sparrow.platform.common.data.ControlType
import com.github.adamyork.sparrow.platform.common.data.LifeCycleState
import com.github.adamyork.sparrow.platform.common.data.ViewPort
import com.github.adamyork.sparrow.platform.common.data.map.GameMap
import com.github.adamyork.sparrow.platform.common.data.map.GameMapState
import com.github.adamyork.sparrow.platform.engine.Engine
import com.github.adamyork.sparrow.platform.engine.Particles
import com.github.adamyork.sparrow.platform.engine.data.DrawResult
import com.github.adamyork.sparrow.platform.gui.data.StateElements
import com.github.adamyork.sparrow.platform.gui.data.UiState
import com.github.adamyork.sparrow.platform.gui.data.ScreenDimensions
import com.github.adamyork.sparrow.platform.service.*
import com.github.adamyork.sparrow.platform.service.data.ImageAsset
import com.github.adamyork.sparrow.platform.service.data.LoadingTask
import com.github.adamyork.sparrow.platform.service.v1.LoadingViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class UiController(
    private val assetService: AssetService,
    private val engine: Engine,
    private val particles: Particles,
    private val scoreService: ScoreService,
    private val runtimeService: RuntimeService,
    private val wavService: WavService,
    private val screenDimensionsService: ScreenDimensionsService
) : LoadingProgressListener {

    private val logger = KotlinLogging.logger {}
    private val viewModel = LoadingViewModel()

    val stateElements: StateElements = StateElements.emptyStateElements
    var splashImage: ImageBitmap? by mutableStateOf(null)
        private set
    var endingImage: ImageBitmap? by mutableStateOf(null)
        private set
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
                "map" to { assetService.loadMap(0, this@UiController) },
                "player" to { assetService.loadPlayer() },
                "collectible item" to { assetService.loadItem(0) },
                "finish item" to { assetService.loadItem(1) },
                "blocker enemy" to { assetService.loadEnemy(0) },
                "shooter enemy" to { assetService.loadEnemy(1) },
                "game audio" to { assetService.loadAudio(this@UiController) },
                "font" to { assetService.prepareFont() }
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
            val gameMap = loadedAssets.getValue("map") as GameMap
            val viewPort = createInitialViewPort(screenDimensions)
            val playerAsset = loadedAssets.getValue("player") as ImageAsset
            val collectibleAsset = loadedAssets.getValue("collectible item") as ImageAsset
            val finishAsset = loadedAssets.getValue("finish item") as ImageAsset
            val blockerAsset = loadedAssets.getValue("blocker enemy") as ImageAsset
            val shooterAsset = loadedAssets.getValue("shooter enemy") as ImageAsset
            val player = engine.createDefaultPlayer(playerAsset)
            val font = loadedAssets.getValue("font")
            stateElements.viewPort = viewPort
            stateElements.player = player
            stateElements.gameMap = gameMap
            stateElements.splashImage = splashImage
            stateElements.endingImage = endingImage
            this.splashImage = splashImage
            this.endingImage = endingImage
            stateElements.playerAsset = playerAsset
            stateElements.mapItemCollectibleAsset = collectibleAsset
            stateElements.mapItemFinishAsset = finishAsset
            stateElements.mapEnemyBlockerAsset = blockerAsset
            stateElements.mapEnemyShooterAsset = shooterAsset
            stateElements.scoreLabel = "Score: --"
            stateElements.totalLabel = "Total: --"
            stateElements.remainingLabel = "Remaining: --"
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
            engine.initialize(gameMap, gameMap.collisionAsset, player, font)
            particles.populateColorMap(assetService)
            scoreService.gameMapItem = gameMap.items
            refreshScoreLabels()
            runtimeService.gameMapState = gameMap.state
            runtimeService.lifeCycleState = LifeCycleState.INITIALIZED
            logger.info { "splash loaded and game initialized" }
        }.onFailure { logger.error { "init failed $it" } }
    }

    private fun refreshScoreLabels() {
        val total = scoreService.getTotal()
        val remaining = scoreService.getRemaining()
        val score = (total - remaining).coerceAtLeast(0)
        stateElements.scoreLabel = "Score: $score"
        stateElements.totalLabel = "Total: $total"
        stateElements.remainingLabel = "Remaining: $remaining"
    }

    override fun onTaskCompleted(taskId: String) {
        val mappedTaskId = LoadingViewModel.mapKeyToTaskId(taskId)
        logger.info { "task id is $taskId" }
        logger.info { "mapped key task is $mappedTaskId" }
        viewModel.onTaskCompleted(mappedTaskId)
    }

    fun allTasksCompleted(): Boolean = loadingTasks.all { it.isCompleted }

    fun start() {
        if (runtimeService.gameMapState == GameMapState.COMPLETED) {
            reset()
        }
        runtimeService.lifeCycleState = LifeCycleState.RUNNING
        wavService.playBackgroundAudio()
    }

    fun tick(timestamp: Double): UiState {
        val currentFps = runtimeService.getFps()
        val elements = stateElements
        if (runtimeService.lifeCycleState != LifeCycleState.INITIALIZING) {
            runtimeService.gameMapState = elements.gameMap.state
        }
        val statusText = assetService.getTextForGameState(runtimeService.gameMapState)
        if (runtimeService.lifeCycleState != LifeCycleState.RUNNING || runtimeService.lifeCycleState == LifeCycleState.INITIALIZING) {
            return UiState(
                drawResult = DrawResult.EMPTY_DRAW_RESULT,
                fpsLabel = "FPS: ${currentFps.toInt()}",
                gameStatusLabel = statusText.message,
                gameStatusLabelColor = statusText.color,
                scoreLabel = elements.scoreLabel,
                totalLabel = elements.totalLabel,
                remainingLabel = elements.remainingLabel,
                gameMapState = runtimeService.gameMapState,
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
        runtimeService.gameMapState = currentGameState
        if (currentGameState == GameMapState.COMPLETED && runtimeService.lifeCycleState == LifeCycleState.RUNNING) {
            pause()
        }
        return UiState(
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
        val elements = stateElements
        if (runtimeService.lifeCycleState == LifeCycleState.INITIALIZING) {
            return
        }
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
        runtimeService.lifeCycleState = when {
            runtimeService.gameMapState == GameMapState.COMPLETED -> LifeCycleState.COMPLETED
            runtimeService.lifeCycleState != LifeCycleState.INITIALIZING -> LifeCycleState.PAUSED
            else -> LifeCycleState.INITIALIZING
        }
    }

    fun reset() {
        logger.info { "reset game" }
        stateElements.player = engine.createDefaultPlayer(stateElements.playerAsset)
        stateElements.gameMap.reset(
            stateElements.mapItemCollectibleAsset,
            stateElements.mapItemFinishAsset,
            stateElements.mapEnemyBlockerAsset,
            stateElements.mapEnemyShooterAsset,
            assetService
        )
        stateElements.viewPort = createInitialViewPort(screenDimensionsService.getScreenDimensions())
        scoreService.gameMapItem = stateElements.gameMap.items
        refreshScoreLabels()
        runtimeService.reset()
        runtimeService.gameMapState = stateElements.gameMap.state
        runtimeService.lifeCycleState = LifeCycleState.RUNNING
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
