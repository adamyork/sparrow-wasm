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
import com.github.adamyork.sparrow.platform.gui.data.ScreenDimensions
import com.github.adamyork.sparrow.platform.gui.data.StateElements
import com.github.adamyork.sparrow.platform.gui.data.UiState
import com.github.adamyork.sparrow.platform.service.*
import com.github.adamyork.sparrow.platform.service.data.ImageAsset
import com.github.adamyork.sparrow.platform.service.data.LoadingTask
import com.github.adamyork.sparrow.platform.service.v1.LoadingViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlin.math.round
import kotlin.time.TimeSource

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

    private companion object {
        const val TICK_TRACE_SAMPLE_FRAMES: Int = 120
    }

    private val logger = KotlinLogging.logger {}
    private val viewModel = LoadingViewModel()
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastLoggedScoreSnapshot: Triple<Int, Int, Int>? = null
    private var tickTraceFrameCount: Int = 0
    private var tickTraceUpdateMsTotal: Double = 0.0
    private var tickTraceDrawMsTotal: Double = 0.0
    private var tickTraceTotalMsTotal: Double = 0.0
    private var tickTraceMaxTotalMs: Double = 0.0

    val stateElements: StateElements = StateElements.emptyStateElements
    var splashImage: ImageBitmap? by mutableStateOf(null)
        private set
    var endingImage: ImageBitmap? by mutableStateOf(null)
        private set
    val loadingTasks: List<LoadingTask>
        get() = viewModel.loadingTasks

    suspend fun initializeGame() {
        runCatching {
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
                    async(Dispatchers.Default) {
                        try {
                            val value = loader()
                            val mappedTaskId = LoadingViewModel.mapKeyToTaskId(key)
                            if (mappedTaskId.isNotBlank()) {
                                withContext(Dispatchers.Main) {
                                    viewModel.onTaskCompleted(mappedTaskId)
                                }
                            }
                            key to value
                        } catch (failure: Throwable) {
                            val mappedTaskId = LoadingViewModel.mapKeyToTaskId(key)
                            if (mappedTaskId.isNotBlank()) {
                                withContext(Dispatchers.Main) {
                                    viewModel.onTaskFailed(mappedTaskId, failure)
                                }
                            }
                            throw failure
                        }
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

            withContext(Dispatchers.Default) {
                gameMap.generateMapItems(
                    collectibleAsset,
                    finishAsset,
                    assetService
                )
                gameMap.generateMapEnemies(
                    blockerAsset,
                    shooterAsset,
                    assetService
                )
                engine.initialize(gameMap, gameMap.collisionAsset, player, font)
                particles.populateColorMap(assetService)
            }

            withContext(Dispatchers.Main) {
                stateElements.viewPort = viewPort
                stateElements.player = player
                stateElements.gameMap = gameMap
                stateElements.splashImage = splashImage
                stateElements.endingImage = endingImage
                this@UiController.splashImage = splashImage
                this@UiController.endingImage = endingImage
                stateElements.playerAsset = playerAsset
                stateElements.mapItemCollectibleAsset = collectibleAsset
                stateElements.mapItemFinishAsset = finishAsset
                stateElements.mapEnemyBlockerAsset = blockerAsset
                stateElements.mapEnemyShooterAsset = shooterAsset
                stateElements.scoreLabel = "Score: --"
                stateElements.totalLabel = "Total: --"
                stateElements.remainingLabel = "Remaining: --"
                scoreService.gameMapItem = gameMap.items
                refreshScoreLabels()
                runtimeService.gameMapState = gameMap.state
                runtimeService.lifeCycleState = LifeCycleState.INITIALIZED
            }
        }.onFailure { failure ->
            logger.error(failure) {
                "initializeGame failed: ${failure::class.simpleName}: ${failure.message ?: "no message"}"
            }
        }
    }

    private fun refreshScoreLabels() {
        val total = scoreService.getTotal()
        val remaining = scoreService.getRemaining()
        val score = (total - remaining).coerceAtLeast(0)
        val scoreSnapshot = Triple(score, total, remaining)
        if (lastLoggedScoreSnapshot != scoreSnapshot) {
            logger.info { "Score changed: score=$score total=$total remaining=$remaining" }
            lastLoggedScoreSnapshot = scoreSnapshot
        }
        stateElements.scoreLabel = "Score: $score"
        stateElements.totalLabel = "Total: $total"
        stateElements.remainingLabel = "Remaining: $remaining"
    }

    override fun onTaskCompleted(taskId: String) {
        val mappedTaskId = LoadingViewModel.mapKeyToTaskId(taskId)
        if (mappedTaskId.isBlank()) return
        uiScope.launch {
            viewModel.onTaskCompleted(mappedTaskId)
        }
    }

    override fun onTaskFailed(taskId: String, cause: Throwable?) {
        val mappedTaskId = LoadingViewModel.mapKeyToTaskId(taskId)
        if (mappedTaskId.isBlank()) return
        uiScope.launch {
            viewModel.onTaskFailed(mappedTaskId, cause)
        }
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
        val traceEnabled = logger.isTraceEnabled()
        val tickStartMark = if (traceEnabled) TimeSource.Monotonic.markNow() else null
        val elements = stateElements
        if (runtimeService.lifeCycleState != LifeCycleState.INITIALIZING) {
            runtimeService.gameMapState = elements.gameMap.state
        }
        val statusText = assetService.getTextForGameState(runtimeService.gameMapState)
        val fpsLabel = { "FPS: ${runtimeService.getFps().toInt()}" }
        if (runtimeService.lifeCycleState != LifeCycleState.RUNNING || runtimeService.lifeCycleState == LifeCycleState.INITIALIZING) {
            return UiState(
                drawResult = DrawResult.EMPTY_DRAW_RESULT,
                fpsLabel = fpsLabel(),
                gameStatusLabel = statusText.message,
                gameStatusLabelColor = statusText.color,
                scoreLabel = elements.scoreLabel,
                totalLabel = elements.totalLabel,
                remainingLabel = elements.remainingLabel,
                gameMapState = runtimeService.gameMapState,
                completionTransitionRequested = false
            )
        }
        val updateStartMark = if (traceEnabled) TimeSource.Monotonic.markNow() else null
        val collisionBoundaries = engine.getCollisionBoundaries(elements.player)
        engine.managePlayer(elements.player, collisionBoundaries)
        engine.manageViewport(elements.player, elements.viewPort)
        engine.manageMap(elements.player, elements.gameMap, elements.viewPort)
        engine.manageEnemyAndItemCollision(elements.player, elements.gameMap, elements.viewPort)

        scoreService.gameMapItem = elements.gameMap.items
        refreshScoreLabels()
        val updateMs = updateStartMark?.let { nanosToMs(it.elapsedNow().inWholeNanoseconds) } ?: 0.0

        val drawStartMark = if (traceEnabled) TimeSource.Monotonic.markNow() else null
        val drawResult = engine.draw(elements.gameMap, elements.viewPort, elements.player, timestamp)
        val drawMs = drawStartMark?.let { nanosToMs(it.elapsedNow().inWholeNanoseconds) } ?: 0.0
        wavService.playNext()
        val currentGameState = elements.gameMap.state
        runtimeService.gameMapState = currentGameState
        if (currentGameState == GameMapState.COMPLETED && runtimeService.lifeCycleState == LifeCycleState.RUNNING) {
            pause()
        }
        val uiState = UiState(
            drawResult = drawResult,
            fpsLabel = fpsLabel(),
            gameStatusLabel = statusText.message,
            gameStatusLabelColor = statusText.color,
            scoreLabel = elements.scoreLabel,
            totalLabel = elements.totalLabel,
            remainingLabel = elements.remainingLabel,
            gameMapState = currentGameState,
            completionTransitionRequested = currentGameState == GameMapState.COMPLETED
        )
        if (traceEnabled && tickStartMark != null) {
            val totalTickMs = nanosToMs(tickStartMark.elapsedNow().inWholeNanoseconds)
            recordTickTrace(updateMs = updateMs, drawMs = drawMs, totalTickMs = totalTickMs)
        }
        return uiState
    }

    private fun recordTickTrace(updateMs: Double, drawMs: Double, totalTickMs: Double) {
        tickTraceFrameCount += 1
        tickTraceUpdateMsTotal += updateMs
        tickTraceDrawMsTotal += drawMs
        tickTraceTotalMsTotal += totalTickMs
        tickTraceMaxTotalMs = tickTraceMaxTotalMs.coerceAtLeast(totalTickMs)
        if (tickTraceFrameCount < TICK_TRACE_SAMPLE_FRAMES) {
            return
        }
        val sampleFrames = tickTraceFrameCount.toDouble()
        logger.trace {
            "TickTiming frames=$tickTraceFrameCount " +
                "avgMs(update=${formatTraceMs(tickTraceUpdateMsTotal / sampleFrames)}, " +
                "draw=${formatTraceMs(tickTraceDrawMsTotal / sampleFrames)}, " +
                "total=${formatTraceMs(tickTraceTotalMsTotal / sampleFrames)}) " +
                "maxTotalMs=${formatTraceMs(tickTraceMaxTotalMs)}"
        }
        tickTraceFrameCount = 0
        tickTraceUpdateMsTotal = 0.0
        tickTraceDrawMsTotal = 0.0
        tickTraceTotalMsTotal = 0.0
        tickTraceMaxTotalMs = 0.0
    }

    private fun nanosToMs(valueNanos: Long): Double = valueNanos.toDouble() / 1_000_000.0

    private fun formatTraceMs(value: Double): String {
        val roundedValue = round(value * 1000.0) / 1000.0
        return roundedValue.toString()
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
