package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.AudioQueue
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.ViewPort
import com.github.adamyork.sparrow.platform.common.data.map.GameMapState
import com.github.adamyork.sparrow.platform.common.data.item.Item
import com.github.adamyork.sparrow.platform.common.data.map.GameMap
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.common.data.player.PlayerJumpingState
import com.github.adamyork.sparrow.platform.common.data.player.PlayerMovingState
import com.github.adamyork.sparrow.platform.engine.Collision
import com.github.adamyork.sparrow.platform.engine.EngineException
import com.github.adamyork.sparrow.platform.engine.Particles
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.data.CommonImage
import com.github.adamyork.sparrow.platform.service.AssetService
import com.github.adamyork.sparrow.platform.service.PhysicsSettingsService
import com.github.adamyork.sparrow.platform.service.RuntimeService
import com.github.adamyork.sparrow.platform.service.ScoreService
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import com.github.adamyork.sparrow.wasm.gui.WasmJsUiParticleLayer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.window
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.Canvas
import org.w3c.dom.HTMLCanvasElement
import kotlin.time.Duration.Companion.milliseconds

/**
 * Experimental wasm engine that renders particles through WebGL overlay buffers.
 */
@AppScope
@Inject
class WasmJsGpuEngine(
    physics: Physics,
    collision: Collision,
    particles: Particles,
    private val particleLayer: WasmJsUiParticleLayer,
    private val physicsSettingsService: PhysicsSettingsService,
    audioQueue: AudioQueue,
    scoreService: ScoreService,
    assetService: AssetService,
    runtimeService: RuntimeService,
    platformInterop: PlatformInterop
) : WasmJsEngine(
    physics,
    collision,
    particles,
    audioQueue,
    scoreService,
    assetService,
    runtimeService,
    platformInterop
) {

    private companion object {
        const val OVERLAY_CANVAS_WAIT_TIMEOUT_MS = 3_000
        const val OVERLAY_CANVAS_WAIT_STEP_MS = 16L
    }

    private val logger = KotlinLogging.logger {}

    private val gpuParticleRenderer = WasmJsGpuParticleRenderer()
    private val gpuParticleBufferCapacity = Particles.DEFAULT_GPU_PARTICLE_CAPACITY
    private val gpuParticleSpawnBuffer = particles.createGpuParticleComputeBuffer(gpuParticleBufferCapacity)
    private var gpuRendererReady: Boolean = false
    private var nextGpuSpawnSlot: Int = 0
    private var previousGpuWrittenSlots: List<Int> = emptyList()

    override suspend fun initialize(
        gameMap: GameMap,
        collisionImageAndBytes: ImageAndBytes,
        player: Player,
        font: Any
    ) {
        super.initialize(gameMap, collisionImageAndBytes, player, font)
        val overlayCanvas = waitForOverlayCanvasReady()
            ?: throw IllegalStateException("WebGPU particle overlay canvas could not be created")
        gpuRendererReady = gpuParticleRenderer.initialize(
            maxParticleCapacity = gpuParticleBufferCapacity,
            particlesShaderSource = assetService.particleShaderSource,
            overlayCanvas = overlayCanvas,
            mapItemTextureBytes = gameMap.items.firstOrNull()?.imageAndBytes?.bytes ?: byteArrayOf(),
            mapItemFirstCellWidth = gameMap.items.firstOrNull()?.width ?: 1,
            mapItemFirstCellHeight = gameMap.items.firstOrNull()?.height ?: 1
        )
        if (!gpuRendererReady) {
            val dpr = window.devicePixelRatio
            val userAgent = window.navigator.userAgent
            throw EngineException(
                "WebGPU particle renderer failed to initialize; WasmJsGpuEngine requires WebGPU. " +
                        "dpr=$dpr, userAgent=$userAgent"
            )
        }
        logger.info { "GPU particle renderer initialized" }
    }

    override fun manageMapParticles(player: Player, gameMap: GameMap, viewPort: ViewPort) {
        if (player.moving == PlayerMovingState.MOVING && player.jumping == PlayerJumpingState.GROUNDED) {
            particles.applyDustParticles(player, gameMap.particles)
        }
        val allCollectiblesFound = scoreService.allFound()
        gameMap.state = when (gameMap.state) {
            GameMapState.COLLECTING if allCollectiblesFound -> GameMapState.COMPLETING
            GameMapState.COMPLETING if !allCollectiblesFound -> GameMapState.COLLECTING
            else -> gameMap.state
        }
        val spawnWriteResult = particles.writeGpuParticleSpawnBuffer(
            player,
            gameMap.particles,
            gpuParticleSpawnBuffer,
            gpuParticleBufferCapacity,
            startSlot = nextGpuSpawnSlot,
            previouslyWrittenSlots = previousGpuWrittenSlots
        )
        val spawnedParticleCount = spawnWriteResult.activeCount
        previousGpuWrittenSlots = spawnWriteResult.writtenSlots
        if (spawnedParticleCount > 0) {
            nextGpuSpawnSlot = (nextGpuSpawnSlot + spawnedParticleCount) % gpuParticleBufferCapacity
        }
        gameMap.particles.clear()
        val deltaTimeSeconds = runtimeService.getDeltaTimeSeconds()
        gpuParticleRenderer.updateGpuParticleBuffer(
            activeParticleCount = spawnedParticleCount,
            sourceBuffer = gpuParticleSpawnBuffer,
            dirtySlotRanges = spawnWriteResult.dirtySlotRanges,
            deltaTimeSeconds = deltaTimeSeconds,
            player = player,
            gameMap = gameMap,
            viewPort = viewPort,
            collision = collision,
            audioQueue = audioQueue,
            particles = particles,
            playerX = player.x.toFloat(),
            playerY = player.y.toFloat(),
            playerWidth = player.width.toFloat(),
            playerHeight = player.height.toFloat(),
            gravity = physicsSettingsService.gravity.toFloat(),
            tickTargetPerSecond = assetService.appProperties.engine.tickTargetPerSec,
            speedCoefficient = physicsSettingsService.collisionParticleSpeedCoefficient.toFloat(),
            dustSpeedCoefficient = physicsSettingsService.dustParticleSpeedCoefficient.toFloat(),
            projectileSpeed = physicsSettingsService.projectileSpeed.toFloat(),
            mapItemReturnSpeed = physicsSettingsService.mapItemReturnParticleSpeed.toFloat(),
            mapItemReturnMinTravelDist = physicsSettingsService.mapItemReturnParticleMinTravelDist.toFloat()
        )
    }

    override fun drawParticles(
        map: GameMap,
        viewPort: ViewPort,
        canvas: Canvas,
        mapItem: Item?,
        mapItemImage: CommonImage?
    ) {
        gpuParticleRenderer.draw(
            viewPort = viewPort,
            sizeMultiplier = physicsSettingsService.collisionParticleSizeMultiplier
        )
    }


    private suspend fun waitForOverlayCanvasReady(): HTMLCanvasElement? {
        val deadline = window.performance.now() + OVERLAY_CANVAS_WAIT_TIMEOUT_MS
        while (window.performance.now() < deadline) {
            val overlayCanvas = particleLayer.getOverlayCanvas()
            if (overlayCanvas != null) {
                return overlayCanvas
            }
            delay(OVERLAY_CANVAS_WAIT_STEP_MS.milliseconds)
        }
        return particleLayer.getOverlayCanvas()
    }

}

