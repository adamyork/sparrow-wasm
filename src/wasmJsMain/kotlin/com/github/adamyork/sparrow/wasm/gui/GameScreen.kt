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

    fun next(): DrawResult {
        if (statusProvider.running) {
            val lastPaintMs = statusProvider.lastPaintTime
            val nextPaintTimeMs = Clock.System.now().toEpochMilliseconds()
            val deltaTime = nextPaintTimeMs - lastPaintMs
            val fpsMaxDeltaTimeMs = 1000 / assetService.gameConfig.engine.fps.max
            //if (deltaTime >= fpsMaxDeltaTimeMs) {
            //logger.info { "getting collision boundaries" }
            val collisionBoundaries = engine.getCollisionBoundaries(player)
            //logger.info { "managing player" }
            player = engine.managePlayer(player, collisionBoundaries)
            //logger.info { "managing viewport" }
            viewPort = engine.manageViewport(player, viewPort)
            //logger.info { "managing map" }
            gameMap = engine.manageMap(player, gameMap)
            //logger.info { "handling collision" }
            val nextPlayerAndMap = engine.manageEnemyAndItemCollision(player, gameMap, viewPort)
            player = nextPlayerAndMap.first
            gameMap = nextPlayerAndMap.second
            scoreService.gameMapItem = gameMap.items
            statusProvider.lastPaintTime = nextPaintTimeMs
            logger.info { "drawing" }
            return engine.draw(gameMap, viewPort, player)
            //} else {
            //return null
            //}
        }
        return DrawResult(null, null)
    }

    @Composable
    override fun build() {
        val composeScreenLayer = remember { ComposeScreenLayer() }
        var fpsLabel by remember { mutableStateOf("FPS: --") }
        var isRunning by remember { mutableStateOf(false) }

        LaunchedEffect(isRunning) {
            if (isRunning) {
                var frameId = 0
                fun loop(time: Double) {
                    val drawResult = next()
                    drawResult.backgroundSurface?.let { surface ->
                        composeScreenLayer.drawBackground(surface.makeImageSnapshot().toComposeImageBitmap())
                    }
                    drawResult.foregroundSurface?.let { surface ->
                        composeScreenLayer.drawForeground(surface.makeImageSnapshot().toComposeImageBitmap())
                    }
                    val currentFps = statusProvider.getFps()
                    fpsLabel = "FPS: ${currentFps.toInt()}"
                    println("Frame rendered at: $time")
                    frameId = window.requestAnimationFrame { timestamp ->
                        loop(timestamp)
                    }
                }
                frameId = window.requestAnimationFrame { timestamp ->
                    loop(timestamp)
                }
                try {
                    awaitCancellation()
                } finally {
                    window.cancelAnimationFrame(frameId)
                }
            }
        }

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
                composeScreenLayer.drawBackground(loadedImage)
            }.onFailure { failure ->
                logger.error { "init failed $failure" }
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
}
