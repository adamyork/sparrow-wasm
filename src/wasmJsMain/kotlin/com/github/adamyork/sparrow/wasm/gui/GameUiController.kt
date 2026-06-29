package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.github.adamyork.sparrow.wasm.common.AudioQueue
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.common.data.Cell
import com.github.adamyork.sparrow.wasm.common.data.ControlAction
import com.github.adamyork.sparrow.wasm.common.data.ControlType
import com.github.adamyork.sparrow.wasm.common.data.Direction
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadata
import com.github.adamyork.sparrow.wasm.common.data.GameElementCollisionState
import com.github.adamyork.sparrow.wasm.common.data.GameElementState
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.common.data.player.PlayerJumpingState
import com.github.adamyork.sparrow.wasm.common.data.player.PlayerMovingState
import com.github.adamyork.sparrow.wasm.engine.data.DrawResult
import com.github.adamyork.sparrow.wasm.engine.Engine
import com.github.adamyork.sparrow.wasm.engine.Particles
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
import kotlin.time.Clock

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
    private val audioQueue: AudioQueue
) : LoadingProgressListener {

    private val logger = KotlinLogging.logger {}
    private val viewModel = LoadingViewModel()

    lateinit var viewPort: ViewPort
    lateinit var player: Player
    lateinit var gameMap: GameMap
    lateinit var playerAsset: ImageAsset
    lateinit var mapItemCollectibleAsset: ImageAsset
    lateinit var mapItemFinishAsset: ImageAsset
    lateinit var mapEnemyBlockerAsset: ImageAsset
    lateinit var mapEnemyShooterAsset: ImageAsset

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
            val loadedImage = assetService.loadBufferedImageAsync("https://sparrow-assets.pages.dev/splash.png").also {
                viewModel.onTaskCompleted("splash")
            }

            viewPort = ViewPort(
                assetService.gameConfig.viewport.x,
                assetService.gameConfig.viewport.y,
                0,
                0,
                assetService.gameConfig.viewport.width,
                assetService.gameConfig.viewport.height
            )

            coroutineScope {
                val deferredAssets = listOf(
                    async { "map" to (assetService.loadMap(0, this@GameUiController) as Any) },
                    async { "player" to (assetService.loadPlayer() as Any) },
                    async { "collectible item" to (assetService.loadItem(0) as Any) },
                    async { "finish item" to (assetService.loadItem(1) as Any) },
                    async { "blocker enemy" to (assetService.loadEnemy(0) as Any) },
                    async { "shooter enemy" to (assetService.loadEnemy(1) as Any) },
                    async { "game audio" to (assetService.loadAudio(this@GameUiController) as Any) }
                )
                val loadedAssets = deferredAssets
                    .map { deferred ->
                        async {
                            val (key, value) = deferred.await()
                            viewModel.onTaskCompleted(LoadingViewModel.mapKeyToTaskId(key))
                            key to value
                        }
                    }
                    .awaitAll()
                    .toMap()
                gameMap = loadedAssets["map"] as GameMap
                playerAsset = loadedAssets["player"] as ImageAsset
                mapItemCollectibleAsset = loadedAssets["collectible item"] as ImageAsset
                mapItemFinishAsset = loadedAssets["finish item"] as ImageAsset
                mapEnemyBlockerAsset = loadedAssets["blocker enemy"] as ImageAsset
                mapEnemyShooterAsset = loadedAssets["shooter enemy"] as ImageAsset
            }

            player = createDefaultPlayer()
            gameMap.generateMapItems(mapItemCollectibleAsset, mapItemFinishAsset, assetService)
            gameMap.generateMapEnemies(mapEnemyBlockerAsset, mapEnemyShooterAsset, assetService)
            engine.setCollisionBufferedImage(gameMap.collisionAsset)
            particles.populateColorMap(assetService)
            scoreService.gameMapItem = gameMap.items
            isInitialized = true
            statusProvider.lastPaintTime = Clock.System.now().toEpochMilliseconds()
            logger.info { "splash loaded and game initialized" }
            loadedImage
        }.onFailure { failure ->
            logger.error { "init failed $failure" }
        }.getOrNull()
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
        player = createDefaultPlayer()
        gameMap.reset(
            mapItemCollectibleAsset,
            mapItemFinishAsset,
            mapEnemyBlockerAsset,
            mapEnemyShooterAsset,
            assetService
        )
        viewPort = ViewPort(
            assetService.gameConfig.viewport.x,
            assetService.gameConfig.viewport.y,
            0,
            0,
            assetService.gameConfig.viewport.width,
            assetService.gameConfig.viewport.height
        )
        scoreService.gameMapItem = gameMap.items
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
        if (!isInitialized) {
            return
        }
        when (controlType) {
            ControlType.START -> startInput(controlAction)
            ControlType.STOP -> stopInput(controlAction)
        }
    }

    private fun createDefaultPlayer(): Player {
        return Player(
            assetService.gameConfig.player.x,
            assetService.gameConfig.player.y,
            playerAsset.width,
            playerAsset.height,
            GameElementState.ACTIVE,
            FrameMetadata(1, Cell(1, 1, playerAsset.width, playerAsset.height)),
            playerAsset.imageAndBytes,
            0.0,
            0.0,
            PlayerJumpingState.GROUNDED,
            PlayerMovingState.STATIONARY,
            Direction.RIGHT,
            GameElementCollisionState.FREE
        )
    }

    private fun getStatusText(): TextAsset {
        return if (isInitialized) {
            assetService.getTextForGameState(gameMap.state)
        } else {
            TextAsset("Press Start To Begin", Color.Black)
        }
    }

    private fun next(): DrawResult {
        if (!statusProvider.running || !isInitialized) {
            return emptyDrawResult()
        }
        val nextPaintTimeMs = Clock.System.now().toEpochMilliseconds()
        if (statusProvider.atOrUnderFpsMax(nextPaintTimeMs)) {
            val collisionBoundaries = engine.getCollisionBoundaries(player)
            player = engine.managePlayer(player, collisionBoundaries)
            viewPort = engine.manageViewport(player, viewPort)
            gameMap = engine.manageMap(player, gameMap)
            val nextPlayerAndMap = engine.manageEnemyAndItemCollision(player, gameMap, viewPort)
            player = nextPlayerAndMap.first
            gameMap = nextPlayerAndMap.second
            scoreService.gameMapItem = gameMap.items
            statusProvider.lastPaintTime = nextPaintTimeMs
            return engine.draw(gameMap, viewPort, player)
        }
        return emptyDrawResult()
    }

    private fun emptyDrawResult(): DrawResult {
        return DrawResult(
            null,
            null,
            0f,
            0f,
            null,
            0f,
            0f,
            null,
            0f,
            0f,
            0f,
            0f
        )
    }

    private fun startInput(controlAction: ControlAction) {
        when (controlAction) {
            ControlAction.LEFT -> {
                val nextVx = adjustXVelocity(controlAction)
                player = player.copy(moving = PlayerMovingState.MOVING, direction = Direction.LEFT, vx = nextVx)
            }

            ControlAction.RIGHT -> {
                val nextVx = adjustXVelocity(controlAction)
                player = player.copy(moving = PlayerMovingState.MOVING, direction = Direction.RIGHT, vx = nextVx)
            }

            ControlAction.JUMP -> {
                if (player.jumping == PlayerJumpingState.GROUNDED) {
                    audioQueue.queue.add(Sounds.JUMP)
                    player = player.copy(jumping = PlayerJumpingState.INITIAL)
                }
            }
        }
    }

    private fun adjustXVelocity(controlAction: ControlAction): Double {
        if (controlAction == ControlAction.LEFT) {
            if (player.direction == Direction.RIGHT) {
                logger.info { getDirectionChangedLogMessage() }
                return 0.0
            }
            return player.vx
        }
        if (player.direction == Direction.LEFT) {
            logger.info { getDirectionChangedLogMessage() }
            return 0.0
        }
        return player.vx
    }

    private fun getDirectionChangedLogMessage(): String {
        return "direction changed player vx was: ${player.vx} and is now 0"
    }

    private fun stopInput(controlAction: ControlAction) {
        if (controlAction == ControlAction.LEFT && player.direction == Direction.RIGHT) {
            logger.warn { "stop player left called before right started" }
        }
        if (controlAction == ControlAction.RIGHT && player.direction == Direction.LEFT) {
            logger.warn { "stop player right called before left started" }
        }
        if (controlAction == ControlAction.RIGHT) {
            if (player.direction == Direction.RIGHT) {
                player = player.copy(moving = PlayerMovingState.STATIONARY, direction = Direction.RIGHT)
            }
        } else if (controlAction == ControlAction.LEFT) {
            if (player.direction == Direction.LEFT) {
                player = player.copy(moving = PlayerMovingState.STATIONARY, direction = Direction.LEFT)
            }
        }
    }
}

