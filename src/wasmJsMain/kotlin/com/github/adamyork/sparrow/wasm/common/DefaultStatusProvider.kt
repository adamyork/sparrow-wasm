package com.github.adamyork.sparrow.wasm.common

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.service.AssetService
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.Surface
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock

@OptIn(ExperimentalAtomicApi::class)
@AppScope
@Inject
class DefaultStatusProvider(
    val assetService: AssetService
) : StatusProvider {

    override var running: Boolean = false
    override var lastPaintTime: Long = 0L
    override var lastBackgroundComposite: Surface? = null

    override fun getDeltaTime(): Double {
        val targetDeltaTimeMs = 1000 / assetService.gameConfig.engine.fps.max
        val deltaTime = Clock.System.now().toEpochMilliseconds() - lastPaintTime
        if (deltaTime > targetDeltaTimeMs) {
            val deltaTimePercent: Double = (deltaTime - targetDeltaTimeMs).toDouble() / targetDeltaTimeMs.toDouble()
            val numOfFramesDropped = assetService.gameConfig.engine.fps.max * deltaTimePercent
            return if ((assetService.gameConfig.engine.fps.max - numOfFramesDropped.toInt()) < assetService.gameConfig.engine.fps.min) {
                //LOGGER.info("FPS drop detected; long deltaTime $deltaTimePercent percent; frames: $numOfFramesDropped")
                1.0 + deltaTimePercent
            } else {
                1.0
            }
        }
        return 1.0
    }

    fun reset() {
        lastPaintTime = 0L
        lastBackgroundComposite = null
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
