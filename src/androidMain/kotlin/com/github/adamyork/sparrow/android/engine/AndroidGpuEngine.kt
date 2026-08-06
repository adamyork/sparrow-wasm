package com.github.adamyork.sparrow.android.engine

import com.github.adamyork.sparrow.android.engine.data.AndroidPendingGpuFrame
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.AudioQueue
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.ViewPort
import com.github.adamyork.sparrow.platform.common.data.map.GameMap
import com.github.adamyork.sparrow.platform.common.data.map.GameMapState
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.common.data.player.PlayerJumpingState
import com.github.adamyork.sparrow.platform.common.data.player.PlayerMovingState
import com.github.adamyork.sparrow.platform.engine.Collision
import com.github.adamyork.sparrow.platform.engine.Particles
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.data.CommonImage
import com.github.adamyork.sparrow.platform.service.AssetService
import com.github.adamyork.sparrow.platform.service.PhysicsSettingsService
import com.github.adamyork.sparrow.platform.service.RuntimeService
import com.github.adamyork.sparrow.platform.service.ScoreService
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject

/**
 * Android engine variant that follows the wasm GPU particle flow.
 *
 * Particle spawn data is written to a packed GPU-style buffer and consumed by an
 * OpenGL ES compute + render pipeline.
 */
@AppScope
@Inject
class AndroidGpuEngine(
    physics: Physics,
    collision: Collision,
    particles: Particles,
    private val physicsSettingsService: PhysicsSettingsService,
    audioQueue: AudioQueue,
    scoreService: ScoreService,
    assetService: AssetService,
    runtimeService: RuntimeService,
    platformInterop: PlatformInterop
) : AndroidEngine(
    physics,
    collision,
    particles,
    audioQueue,
    scoreService,
    assetService,
    runtimeService,
    platformInterop
) {

    companion object {
        private const val FLOATS_PER_PARTICLE = 16
        private const val IDX_ALIVE = 7
        private const val IDX_KIND = 12
        private const val KIND_DUST = 1f
        private const val ENGINE_DIAGNOSTIC_LOG_EVERY_N_TICKS = 60
    }

    private val logger = KotlinLogging.logger {}

    private val gpuParticleBufferCapacity = Particles.DEFAULT_GPU_PARTICLE_CAPACITY
    private val gpuParticleSpawnBuffer = particles.createGpuParticleComputeBuffer(gpuParticleBufferCapacity)
    private val gpuParticleRuntime = AndroidGpuParticleRuntime()
    private var nextGpuSpawnSlot: Int = 0
    private var previousGpuWrittenSlots: List<Int> = emptyList()
    private var androidPendingGpuFrame: AndroidPendingGpuFrame? = null
    private var diagnosticTickCounter: Int = 0

    override suspend fun initialize(
        gameMap: GameMap,
        collisionImageAndBytes: ImageAndBytes,
        player: Player,
        font: Any
    ) {
        super.initialize(gameMap, collisionImageAndBytes, player, font)
        assetService.loadParticleGlShaders()
        gpuParticleRuntime.enable(
            maxParticleCapacity = gpuParticleBufferCapacity,
            computeShader = assetService.particleComputeShaderSource,
            vertexShader = assetService.particleVertexShaderSource,
            fragmentShader = assetService.particleFragmentShaderSource
        )
        gpuParticleRuntime.setAsActiveRuntime()
        logger.info { "Android GL particle runtime initialized with $gpuParticleBufferCapacity slots" }
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
            player = player,
            mapParticles = gameMap.particles,
            targetBuffer = gpuParticleSpawnBuffer,
            maxParticles = gpuParticleBufferCapacity,
            startSlot = nextGpuSpawnSlot,
            previouslyWrittenSlots = previousGpuWrittenSlots
        )
        val mapDustCount = gameMap.particles.count { it.type.name == "DUST" }
        previousGpuWrittenSlots = spawnWriteResult.writtenSlots
        val spawnedParticleCount = spawnWriteResult.activeCount
        if (spawnedParticleCount > 0) {
            nextGpuSpawnSlot = (nextGpuSpawnSlot + spawnedParticleCount) % gpuParticleBufferCapacity
        }
        diagnosticTickCounter++
        if (diagnosticTickCounter % ENGINE_DIAGNOSTIC_LOG_EVERY_N_TICKS == 0) {
            logger.info {
                "[GPU][Engine] mapParticles=${gameMap.particles.size}, mapDust=$mapDustCount, " +
                    "spawned=$spawnedParticleCount, dirtyRanges=${spawnWriteResult.dirtySlotRanges.size}, " +
                    "bufferDust=${countAliveParticlesByKind(gpuParticleSpawnBuffer, KIND_DUST)}"
            }
        }
        gameMap.particles.clear()
        androidPendingGpuFrame = AndroidPendingGpuFrame(
            deltaTimeSeconds = runtimeService.getDeltaTimeSeconds(),
            gravity = physicsSettingsService.gravity.toFloat(),
            projectileSpeed = physicsSettingsService.projectileSpeed.toFloat(),
            mapItemReturnSpeed = physicsSettingsService.mapItemReturnParticleSpeed.toFloat(),
            mapItemReturnMinTravelDist = physicsSettingsService.mapItemReturnParticleMinTravelDist.toFloat(),
            playerX = player.x.toFloat(),
            playerY = player.y.toFloat(),
            playerWidth = player.width.toFloat(),
            playerHeight = player.height.toFloat()
        )
    }

    override fun drawParticles(
        map: GameMap,
        viewPort: ViewPort,
        canvas: android.graphics.Canvas,
        mapItem: com.github.adamyork.sparrow.platform.common.data.item.Item?,
        mapItemImage: CommonImage?
    ) {
        val frame = androidPendingGpuFrame ?: return
        androidPendingGpuFrame = null
        gpuParticleRuntime.submitFrame(
            sourceBuffer = gpuParticleSpawnBuffer,
            viewPort = viewPort,
            sizeMultiplier = physicsSettingsService.collisionParticleSizeMultiplier,
            deltaTimeSeconds = frame.deltaTimeSeconds,
            gravity = frame.gravity,
            projectileSpeed = frame.projectileSpeed,
            mapItemReturnSpeed = frame.mapItemReturnSpeed,
            mapItemReturnMinTravelDist = frame.mapItemReturnMinTravelDist,
            playerX = frame.playerX,
            playerY = frame.playerY,
            playerWidth = frame.playerWidth,
            playerHeight = frame.playerHeight
        )
    }

    private fun countAliveParticlesByKind(buffer: FloatArray, kind: Float): Int {
        if (buffer.isEmpty()) return 0
        var count = 0
        var base = 0
        while ((base + IDX_KIND) < buffer.size) {
            val alive = buffer[base + IDX_ALIVE] > 0.5f
            if (alive && buffer[base + IDX_KIND] == kind) {
                count++
            }
            base += FLOATS_PER_PARTICLE
        }
        return count
    }

}
