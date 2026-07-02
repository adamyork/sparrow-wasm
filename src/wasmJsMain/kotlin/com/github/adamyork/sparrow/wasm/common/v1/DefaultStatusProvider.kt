package com.github.adamyork.sparrow.wasm.common.v1

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.service.AssetService
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class DefaultStatusProvider(
    val assetService: AssetService
) : StatusProvider {

    private companion object {
        const val FPS_SAMPLE_WINDOW_MS: Double = 1000.0
    }

    private val logger = KotlinLogging.logger {}

    override var running: Boolean = false
    override var lastPaintTime: Double = 0.0

    private var fpsWindowStartTime: Double = 0.0
    private var fpsFrameCountInWindow: Int = 0
    private var lastObservedPaintTime: Double = 0.0
    private var cachedFps: Double = 0.0
    private var excessTime: Double = 0.0
    private var currentFrameTime: Double = 0.0

    override fun setCurrentFrameTime(timestamp: Double) {
        this.currentFrameTime = timestamp
    }

    override fun getCurrentFrameTime(): Double {
        return currentFrameTime
    }

    override fun getDeltaTimeCoefficient(): Double {
        val maxFps = assetService.gameConfig.engine.fps.max.toDouble()
        val targetDeltaTimeMs = 1000.0 / maxFps

        // Use 0.0 to compare with Double
        if (lastPaintTime <= 0.0 || currentFrameTime <= 0.0) {
            return 1.0
        }

        val actualDeltaTimeMs = currentFrameTime - lastPaintTime
        return if (actualDeltaTimeMs > targetDeltaTimeMs) {
            val multiplier = actualDeltaTimeMs / targetDeltaTimeMs
            logger.debug { "FPS drop: target=${targetDeltaTimeMs}ms, actual=${actualDeltaTimeMs}ms, coeff=${multiplier}" }
            multiplier.coerceAtMost(2.0)
        } else {
            1.0
        }
    }

    // Changed parameter to Double to match browser timestamps
    override fun atOrUnderFpsMax(nextPaintTimeMs: Double): Boolean {
        val maxFps = assetService.gameConfig.engine.fps.max.toDouble()
        val targetIntervalMs = 1000.0 / maxFps
        val delta = nextPaintTimeMs - lastPaintTime

        excessTime += delta

        if (excessTime >= targetIntervalMs) {
            excessTime -= targetIntervalMs
            lastPaintTime = nextPaintTimeMs
            return true
        }
        return false
    }

    override fun reset() {
        lastPaintTime = 0.0
        fpsWindowStartTime = 0.0
        fpsFrameCountInWindow = 0
        lastObservedPaintTime = 0.0
        cachedFps = 0.0
        excessTime = 0.0
        currentFrameTime = 0.0
    }

    override fun getFps(): Double {
        val currentPaintTime = lastPaintTime
        if (currentPaintTime <= 0.0) {
            return 0.0
        }

        if (currentPaintTime != lastObservedPaintTime) {
            lastObservedPaintTime = currentPaintTime

            if (fpsWindowStartTime == 0.0) {
                fpsWindowStartTime = currentPaintTime
                fpsFrameCountInWindow = 1
            } else {
                fpsFrameCountInWindow += 1
            }

            val elapsedWindowMs = currentPaintTime - fpsWindowStartTime
            if (elapsedWindowMs >= FPS_SAMPLE_WINDOW_MS) {
                cachedFps = (fpsFrameCountInWindow.toDouble() * 1000.0) / elapsedWindowMs
                fpsWindowStartTime = currentPaintTime
                fpsFrameCountInWindow = 0
            }
        }
        return cachedFps
    }
}