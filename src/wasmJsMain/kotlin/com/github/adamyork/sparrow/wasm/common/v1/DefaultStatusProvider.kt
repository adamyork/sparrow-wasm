package com.github.adamyork.sparrow.wasm.common.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.service.AssetService
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultStatusProvider(
    val assetService: AssetService
) : StatusProvider {

    private companion object {
        const val FPS_SAMPLE_WINDOW_MS: Double = 1000.0
    }


    private var runningState by mutableStateOf(false)
    override var running: Boolean
        get() = runningState
        set(value) {
            runningState = value
        }

    override var lastPaintTime: Double = 0.0
    private var gameMapStateValue by mutableStateOf<GameMapState?>(null)
    override var gameMapState: GameMapState?
        get() = gameMapStateValue
        set(value) {
            gameMapStateValue = value
        }

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
        val targetFps = assetService.appProperties.engine.fps.target.toDouble()
        val targetDeltaTimeMs = 1000.0 / targetFps
        if (lastPaintTime <= 0.0 || currentFrameTime <= 0.0) {
            return 1.0
        }
        val actualDeltaTimeMs = currentFrameTime - lastPaintTime
        val coefficient = actualDeltaTimeMs / targetDeltaTimeMs
        return coefficient.coerceIn(0.5, 2.0)
    }


    override fun reset() {
        lastPaintTime = 0.0
        gameMapState = null
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
