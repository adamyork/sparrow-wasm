package com.github.adamyork.sparrow.wasm.common.v1

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.service.AssetService
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

@AppScope
@Inject
/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class DefaultStatusProvider(
    val assetService: AssetService
) : StatusProvider {

    private companion object {
        const val FPS_SAMPLE_WINDOW_MS: Long = 1000L
    }

    private val logger = KotlinLogging.logger {}

    override var running: Boolean = false
    override var lastPaintTime: Long = 0L

    private var fpsWindowStartTime: Long = 0L
    private var fpsFrameCountInWindow: Int = 0
    private var lastObservedPaintTime: Long = 0L
    private var cachedFps: Double = 0.0

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
        fpsWindowStartTime = 0L
        fpsFrameCountInWindow = 0
        lastObservedPaintTime = 0L
        cachedFps = 0.0
    }

    override fun getFps(): Double {
        val currentPaintTime = lastPaintTime
        if (currentPaintTime <= 0L) {
            return 0.0
        }
        if (currentPaintTime != lastObservedPaintTime) {
            lastObservedPaintTime = currentPaintTime
            if (fpsWindowStartTime == 0L) {
                fpsWindowStartTime = currentPaintTime
                fpsFrameCountInWindow = 1
            } else {
                fpsFrameCountInWindow += 1
            }
            val elapsedWindowMs = currentPaintTime - fpsWindowStartTime
            if (elapsedWindowMs >= FPS_SAMPLE_WINDOW_MS) {
                cachedFps = (fpsFrameCountInWindow * 1000.0) / elapsedWindowMs.toDouble()
                fpsWindowStartTime = currentPaintTime
                fpsFrameCountInWindow = 0
            }
        }
        return cachedFps
    }

}
