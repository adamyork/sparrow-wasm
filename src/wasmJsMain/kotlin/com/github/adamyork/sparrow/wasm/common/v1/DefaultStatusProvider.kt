package com.github.adamyork.sparrow.wasm.common.v1

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.service.AssetService
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

@AppScope
@Inject
class DefaultStatusProvider(
    val assetService: AssetService
) : StatusProvider {

    private val logger = KotlinLogging.logger {}

    override var running: Boolean = false
    override var lastPaintTime: Long = 0L

    override fun getDeltaTimeCoefficient(): Double {
        val targetDeltaTimeMs = 1000 / assetService.gameConfig.engine.fps.max
        val deltaTime = Clock.System.now().toEpochMilliseconds() - lastPaintTime
        if (deltaTime > targetDeltaTimeMs) {
            val deltaTimePercent: Double = (deltaTime - targetDeltaTimeMs).toDouble() / targetDeltaTimeMs.toDouble()
            val numOfFramesDropped = assetService.gameConfig.engine.fps.max * deltaTimePercent
            return if ((assetService.gameConfig.engine.fps.max - numOfFramesDropped.toInt()) < assetService.gameConfig.engine.fps.min) {
                logger.debug { "FPS drop detected; long deltaTime $deltaTimePercent percent; frames: $numOfFramesDropped" }
                1.0 + deltaTimePercent
            } else {
                1.0
            }
        }
        return 1.0
    }

    override fun atOrUnderFpsMax(nextPaintTimeMs: Long): Boolean {
        val maxFps = assetService.gameConfig.engine.fps.max
        val targetIntervalMs = 1000.0 / maxFps
        val elapsedSinceLastPaint = (nextPaintTimeMs - lastPaintTime).toDouble()
        return elapsedSinceLastPaint >= targetIntervalMs
    }

    override fun reset() {
        lastPaintTime = 0L
    }

    override fun getFps(): Double {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val deltaTimeMs = currentTime - lastPaintTime
        return if (deltaTimeMs > 0) {
            1000.0 / deltaTimeMs
        } else {
            0.0
        }
    }

}
