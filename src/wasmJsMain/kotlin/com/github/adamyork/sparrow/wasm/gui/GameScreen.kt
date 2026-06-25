package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.adamyork.sparrow.wasm.DrawResult
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.common.data.ControlAction
import com.github.adamyork.sparrow.wasm.common.data.ControlType
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

@Inject
class GameScreen(
    private val assetService: AssetService,
    private val engine: Engine,
    private val particles: Particles,
    private val scoreService: ScoreService,
    private val statusProvider: StatusProvider,
) : BodyElement {

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
        var isRunning by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            runCatching {
                logger.info { "initializing" }
                assetService.initialize()
                logger.info { "loading splash image" }
                assetService.loadBufferedImageAsync("/splash.png")
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
                val deferredAssets = listOf(
                    async { assetService.loadMap(0) },
                    async { assetService.loadPlayer() },
                    async { assetService.loadItem(0) },
                    async { assetService.loadItem(1) },
                    async { assetService.loadEnemy(0) },
                    async { assetService.loadEnemy(1) }
                )
                val results = deferredAssets.awaitAll()
                gameMap = results[0] as GameMap
                playerAsset = results[1] as ImageAsset
                mapItemCollectibleAsset = results[2] as ImageAsset
                mapItemFinishAsset = results[3] as ImageAsset
                mapEnemyBlockerAsset = results[4] as ImageAsset
                mapEnemyShooterAsset = results[5] as ImageAsset
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
                //LOGGER.info("map items generated")
                gameMap.generateMapEnemies(mapEnemyBlockerAsset, mapEnemyShooterAsset, assetService)
                //LOGGER.info("enemy items generated")
                engine.setCollisionBufferedImage(gameMap.collisionAsset)
                particles.populateColorMap(assetService)
                scoreService.gameMapItem = gameMap.items
                isInitialized = true
                composeScreenLayer.drawSplash(loadedImage)
                statusProvider.lastPaintTime = Clock.System.now().toEpochMilliseconds()
            }.onFailure { failure ->
                logger.error { "init failed $failure" }
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
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .semantics { contentDescription = "start-pause-button-row" }
                        .testTag("start-pause-button-row")
                ) {
                    Button(
                        onClick = {
                            isRunning = true
                            statusProvider.running = true
                        },
                        modifier = Modifier
                            .semantics { contentDescription = "start-button" }
                            .testTag("start-button")
                    ) {
                        Text("Start")
                    }

                    Button(
                        onClick = {
                            // Update the status provider variable directly
                            isRunning = false
                            statusProvider.running = false
                        },
                        modifier = Modifier
                            .semantics { contentDescription = "pause-button" }
                            .testTag("pause-button")
                    ) {
                        Text("Pause")
                    }

                    Button(
                        onClick = { isRunning = false },
                        modifier = Modifier
                            .semantics { contentDescription = "pause-button" }
                            .testTag("pause-button")
                    ) {
                        Text("Pause")
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
                    //LOGGER.info("starting player jump")
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
