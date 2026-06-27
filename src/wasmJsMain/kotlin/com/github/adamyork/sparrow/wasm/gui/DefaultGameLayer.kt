package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.AudioQueue
import com.github.adamyork.sparrow.wasm.engine.DrawResult
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.common.data.ControlAction
import com.github.adamyork.sparrow.wasm.common.data.ControlType
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import com.github.adamyork.sparrow.wasm.data.*
import com.github.adamyork.sparrow.wasm.data.Direction
import com.github.adamyork.sparrow.wasm.data.map.GameMap
import com.github.adamyork.sparrow.wasm.data.player.Player
import com.github.adamyork.sparrow.wasm.data.player.PlayerJumpingState
import com.github.adamyork.sparrow.wasm.data.player.PlayerMovingState
import com.github.adamyork.sparrow.wasm.engine.Engine
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.ScoreService
import com.github.adamyork.sparrow.wasm.service.WavService
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.window
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import me.tatarka.inject.annotations.Inject
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import kotlin.time.Clock

@AppScope
@Inject
class DefaultGameLayer(
    private val assetService: AssetService,
    private val engine: Engine,
    private val particles: Particles,
    private val scoreService: ScoreService,
    private val statusProvider: StatusProvider,
    private val wavService: WavService,
    private val audioQueue: AudioQueue
) : GameLayer {

    private val logger = KotlinLogging.logger {}

    lateinit var viewPort: ViewPort
    lateinit var player: Player
    lateinit var gameMap: GameMap
    lateinit var playerAsset: ImageAsset
    lateinit var mapItemCollectibleAsset: ImageAsset
    lateinit var mapItemFinishAsset: ImageAsset
    lateinit var mapEnemyBlockerAsset: ImageAsset
    lateinit var mapEnemyShooterAsset: ImageAsset
    var isInitialized: Boolean = false

    @Composable
    override fun build() {
        val composeScreenLayer = remember { ComposeScreenLayer() }
        var fpsLabel by remember { mutableStateOf("FPS: --") }
        var gameStatusLabel by remember { mutableStateOf("Starting") }
        var gameStatusLabelColor by remember { mutableStateOf(Color.Black) }
        var scoreLabel by remember { mutableStateOf("Score: --") }
        var totalLabel by remember { mutableStateOf("Total: --") }
        var remainingLabel by remember { mutableStateOf("Remaining: --") }
        var isRunning by remember { mutableStateOf(false) }
        var loadingProgress by remember { mutableStateOf(0f) }
        var loadingLabel by remember { mutableStateOf("Initializing...") }

        LaunchedEffect(Unit) {
            runCatching {
                logger.info { "initializing" }
                loadingLabel = "Initializing game services..."
                assetService.initialize()
                logger.info { "loading splash image" }
                loadingLabel = "Loading splash image..."
                //TODO this needs to go in app YAML
                assetService.loadBufferedImageAsync("https://sparrow-assets.pages.dev/splash.png").also {
                    loadingProgress = 0.30f
                }
            }.onSuccess { loadedImage ->
                logger.info { "splash loaded and game initialized" }
                viewPort = ViewPort(
                    assetService.gameConfig.viewport.x,
                    assetService.gameConfig.viewport.y,
                    0,
                    0,
                    assetService.gameConfig.viewport.width,
                    assetService.gameConfig.viewport.height
                )
                val assetProgressEnd = 0.90f

                val deferredAssets = listOf(
                    async { "map" to (assetService.loadMap(0) as Any) },
                    async { "player" to (assetService.loadPlayer() as Any) },
                    async { "collectible item" to (assetService.loadItem(0) as Any) },
                    async { "finish item" to (assetService.loadItem(1) as Any) },
                    async { "blocker enemy" to (assetService.loadEnemy(0) as Any) },
                    async { "shooter enemy" to (assetService.loadEnemy(1) as Any) },
                    async { "game audio" to (assetService.loadAudio() as Any) }
                )
                loadingLabel = "Loading assets in parallel..."
                val loadedAssets = deferredAssets.awaitAll().toMap(mutableMapOf())
                val completedAssetLoads = 6
                loadingProgress = assetProgressEnd
                loadingLabel = "Loaded assets ($completedAssetLoads/6)"
                gameMap = loadedAssets["map"] as GameMap
                playerAsset = loadedAssets["player"] as ImageAsset
                mapItemCollectibleAsset = loadedAssets["collectible item"] as ImageAsset
                mapItemFinishAsset = loadedAssets["finish item"] as ImageAsset
                mapEnemyBlockerAsset = loadedAssets["blocker enemy"] as ImageAsset
                mapEnemyShooterAsset = loadedAssets["shooter enemy"] as ImageAsset

                loadingLabel = "Preparing game world..."
                player = Player(
                    assetService.gameConfig.player.x,
                    assetService.gameConfig.player.y,
                    playerAsset.width,
                    playerAsset.height,
                    GameElementState.ACTIVE,
                    FrameMetadata(1, Cell(1, 1, playerAsset.width, playerAsset.height)),
                    playerAsset.customImageWrapper,
                    0.0,
                    0.0,
                    PlayerJumpingState.GROUNDED,
                    PlayerMovingState.STATIONARY,
                    Direction.RIGHT,
                    GameElementCollisionState.FREE
                )
                gameMap.generateMapItems(mapItemCollectibleAsset, mapItemFinishAsset, assetService)
                gameMap.generateMapEnemies(mapEnemyBlockerAsset, mapEnemyShooterAsset, assetService)
                engine.setCollisionBufferedImage(gameMap.collisionAsset)
                particles.populateColorMap(assetService)
                scoreService.gameMapItem = gameMap.items
                val total = scoreService.getTotal()
                val remaining = scoreService.getRemaining()
                val score = (total - remaining).coerceAtLeast(0)
                scoreLabel = "Score: $score"
                totalLabel = "Total: $total"
                remainingLabel = "Remaining: $remaining"
                isInitialized = true
                composeScreenLayer.drawSplash(loadedImage)
                loadingProgress = 1.0f
                loadingLabel = "Ready"
                statusProvider.lastPaintTime = Clock.System.now().toEpochMilliseconds()
            }.onFailure { failure ->
                logger.error { "init failed $failure" }
                loadingLabel = "Initialization failed"
            }
        }

        LaunchedEffect(Unit) {
            fun toControlAction(event: KeyboardEvent): ControlAction? {
                return when (event.key.lowercase()) {
                    "arrowleft" -> ControlAction.LEFT
                    "arrowright" -> ControlAction.RIGHT
                    " ", "space", "spacebar" -> ControlAction.JUMP
                    else -> null
                }
            }

            val keyDownListener: (Event) -> Unit = { event ->
                if (event is KeyboardEvent) {
                    val action = toControlAction(event)
                    if (action != null) {
                        event.preventDefault()
                        logger.info { "key down: ${event.key}" }
                        applyInput(ControlType.START, action)
                    }
                }
            }

            val keyUpListener: (Event) -> Unit = { event ->
                if (event is KeyboardEvent) {
                    val action = toControlAction(event)
                    if (action != null) {
                        event.preventDefault()
                        logger.info { "key up: ${event.key}" }
                        applyInput(ControlType.STOP, action)
                    }
                }
            }

            window.addEventListener("keydown", keyDownListener)
            window.addEventListener("keyup", keyUpListener)
            try {
                awaitCancellation()
            } finally {
                window.removeEventListener("keydown", keyDownListener)
                window.removeEventListener("keyup", keyUpListener)
            }
        }

        LaunchedEffect(isRunning) {
            if (isRunning) {
                var frameId: Int
                fun loop() {
                    val drawResult = next()
                    wavService.playNext()
                    drawResult.farGroundBitmap?.let { image ->
                        composeScreenLayer.drawFarGround(
                            image,
                            drawResult.farGroundOffsetX,
                            drawResult.farGroundOffsetY
                        )
                    }
                    drawResult.midGroundBitmap?.let { image ->
                        composeScreenLayer.drawMidGround(
                            image,
                            drawResult.midGroundOffsetX,
                            drawResult.midGroundOffsetY
                        )
                    }
                    drawResult.collisionBitmap?.let { image ->
                        composeScreenLayer.drawCollision(
                            image,
                            drawResult.collisionOffsetX,
                            drawResult.collisionOffsetY
                        )
                    }
                    drawResult.foregroundSurface?.let { surface ->
                        composeScreenLayer.drawForeground(surface.makeImageSnapshot().toComposeImageBitmap())
                    }
                    val currentFps = statusProvider.getFps()
                    fpsLabel = "FPS: ${currentFps.toInt()}"
                    val total = scoreService.getTotal()
                    val remaining = scoreService.getRemaining()
                    val score = (total - remaining).coerceAtLeast(0)
                    scoreLabel = "Score: $score"
                    totalLabel = "Total: $total"
                    remainingLabel = "Remaining: $remaining"
                    gameStatusLabel = assetService.getTextForGameState(gameMap.state).message
                    gameStatusLabelColor = assetService.getTextForGameState(gameMap.state).color
                    frameId = window.requestAnimationFrame { _ ->
                        loop()
                    }
                }
                frameId = window.requestAnimationFrame { _ ->
                    loop()
                }
                try {
                    awaitCancellation()
                } finally {
                    window.cancelAnimationFrame(frameId)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "com.github.adamyork.sparrow.wasm.Main content area"
                }
                .testTag("compose-body-com.github.adamyork.sparrow.wasm.main")
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "com.github.adamyork.sparrow.wasm.Main content stack" }
                    .testTag("compose-body-com.github.adamyork.sparrow.wasm.main-column")
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .semantics { contentDescription = "canvas-with-fps-overlay" }
                        .testTag("canvas-with-fps-overlay")
                ) {
                    composeScreenLayer.build(
                        isRunning = isRunning,
                        onFpsLabelChanged = { nextLabel ->
                            fpsLabel = nextLabel
                        }
                    )
                    Text(
                        text = gameStatusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = gameStatusLabelColor,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .semantics { contentDescription = "centered-top-label" }
                            .testTag("centered-top-label")
                    )

                    if (loadingProgress < 1f) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(260.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = loadingLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .semantics { contentDescription = "game-progress-label" }
                                    .testTag("game-progress-label")
                            )
                            LinearProgressIndicator(
                                progress = { loadingProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "game-progress-bar" }
                                    .testTag("game-progress-bar")
                            )
                        }
                    }

                    Text(
                        text = fpsLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White, // Set text color to white
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .semantics { contentDescription = "FPS label" }
                            .testTag("fps-label")
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .semantics { contentDescription = "score-overlay" }
                            .testTag("score-overlay"),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = scoreLabel, style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Text(text = totalLabel, style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Text(text = remainingLabel, style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .semantics { contentDescription = "start-pause-button-row" }
                        .testTag("start-pause-button-row")
                ) {

                    val focusManager = LocalFocusManager.current

                    Button(
                        onClick = {
                            isRunning = true
                            statusProvider.running = true
                            wavService.playBackgroundAudio()
                            focusManager.clearFocus()
                        },
                        enabled = !isRunning,
                        modifier = Modifier
                            .semantics { contentDescription = "start-button" }
                            .testTag("start-button")
                    ) {
                        Text("Start")
                    }

                    Button(
                        onClick = {
                            isRunning = false
                            statusProvider.running = false
                            focusManager.clearFocus()
                        },
                        enabled = isRunning,
                        modifier = Modifier
                            .semantics { contentDescription = "pause-button" }
                            .testTag("pause-button")
                    ) {
                        Text("Pause")
                    }

                    Button(
                        onClick = {
                            isRunning = false
                            focusManager.clearFocus()
                        },
                        enabled = isRunning,
                        modifier = Modifier
                            .semantics { contentDescription = "reset-button" }
                            .testTag("reset-button")
                    ) {
                        Text("reset")
                    }
                }


            }
        }
    }

    private fun next(): DrawResult {
        if (statusProvider.running) {
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
            } else {
                return DrawResult(
                    null,
                    null,
                    0f,
                    0f, null,
                    0f,
                    0f,
                    null,
                    0f,
                    0f,
                    0f,
                    0f
                )
            }
        }
        return DrawResult(
            null,
            null,
            0f,
            0f, null,
            0f,
            0f,
            null,
            0f,
            0f,
            0f,
            0f
        )
    }

    private fun applyInput(controlType: ControlType, controlAction: ControlAction) {
        when (controlType) {
            ControlType.START -> startInput(controlAction)
            ControlType.STOP -> stopInput(controlAction)
        }
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
            } else {
                return player.vx
            }
        } else {
            if (player.direction == Direction.LEFT) {
                logger.info { getDirectionChangedLogMessage() }
                return 0.0
            } else {
                return player.vx
            }
        }
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
