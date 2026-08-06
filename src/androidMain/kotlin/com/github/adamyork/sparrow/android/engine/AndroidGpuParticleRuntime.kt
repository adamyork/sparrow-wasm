package com.github.adamyork.sparrow.android.engine

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import com.github.adamyork.sparrow.android.engine.data.AndroidGpuParticleFrame
import com.github.adamyork.sparrow.platform.common.data.ViewPort
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * Real OpenGL ES 3.1 particle runtime for Android.
 *
 * Physics is advanced in a compute shader and rendering is performed with instanced draws.
 */
class AndroidGpuParticleRuntime {

    private val logger = KotlinLogging.logger {}

    companion object {
        @Volatile
        private var activeRuntime: AndroidGpuParticleRuntime? = null
        private val activeRuntimeFlow = MutableStateFlow<AndroidGpuParticleRuntime?>(null)

        fun getActiveRuntime(): AndroidGpuParticleRuntime? = activeRuntime
        fun observeActiveRuntime(): StateFlow<AndroidGpuParticleRuntime?> = activeRuntimeFlow
    }

    @Volatile
    private var enabled = false

    @Volatile
    private var maxParticles = 0
    private var computeShaderSource: String = ""
    private var vertexShaderSource: String = ""
    private var fragmentShaderSource: String = ""

    private val latestFrame = AtomicReference<AndroidGpuParticleFrame?>(null)
    private var renderer: AndroidGpuParticleRenderer? = null

    fun setAsActiveRuntime() {
        activeRuntime = this
        activeRuntimeFlow.value = this
        logger.info { "[GPU][Runtime] Active runtime published for overlay" }
    }

    @Synchronized
    fun enable(
        maxParticleCapacity: Int,
        computeShader: String,
        vertexShader: String,
        fragmentShader: String
    ) {
        maxParticles = maxParticleCapacity.coerceAtLeast(1)
        computeShaderSource = computeShader
        vertexShaderSource = vertexShader
        fragmentShaderSource = fragmentShader
        enabled = true
        renderer = AndroidGpuParticleRenderer(
            maxParticles = maxParticles,
            frameProvider = { latestFrame.getAndSet(null) },
            computeShaderSource = computeShaderSource,
            vertexShaderSource = vertexShaderSource,
            fragmentShaderSource = fragmentShaderSource
        )
    }

    fun isEnabled(): Boolean = enabled

    fun submitFrame(
        sourceBuffer: FloatArray,
        viewPort: ViewPort,
        sizeMultiplier: Int,
        deltaTimeSeconds: Float,
        gravity: Float,
        projectileSpeed: Float,
        mapItemReturnSpeed: Float,
        mapItemReturnMinTravelDist: Float,
        playerX: Float,
        playerY: Float,
        playerWidth: Float,
        playerHeight: Float
    ) {
        if (!enabled) return
        latestFrame.set(
            AndroidGpuParticleFrame(
                sourceBuffer = sourceBuffer.copyOf(),
                viewPortX = viewPort.x.toFloat(),
                viewPortY = viewPort.y.toFloat(),
                sizeMultiplier = sizeMultiplier,
                deltaTimeSeconds = deltaTimeSeconds.coerceAtLeast(0.0001f),
                gravity = gravity,
                projectileSpeed = projectileSpeed.coerceAtLeast(0f),
                mapItemReturnSpeed = mapItemReturnSpeed.coerceAtLeast(0f),
                mapItemReturnMinTravelDist = mapItemReturnMinTravelDist.coerceAtLeast(0f),
                playerX = playerX,
                playerY = playerY,
                playerWidth = playerWidth.coerceAtLeast(1f),
                playerHeight = playerHeight.coerceAtLeast(1f)
            )
        )
    }

    @Synchronized
    fun createSurfaceView(context: Context): GLSurfaceView {
        logger.info { "[GPU][Runtime] Creating GLSurfaceView (enabled=$enabled, maxParticles=$maxParticles)" }
        val glRenderer = renderer ?: AndroidGpuParticleRenderer(
            maxParticles = maxParticles.coerceAtLeast(1),
            frameProvider = { latestFrame.getAndSet(null) },
            computeShaderSource = computeShaderSource,
            vertexShaderSource = vertexShaderSource,
            fragmentShaderSource = fragmentShaderSource
        ).also { renderer = it }
        return GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            holder.setFormat(PixelFormat.TRANSLUCENT)
            setZOrderOnTop(true)
            preserveEGLContextOnPause = true
            setRenderer(glRenderer)
            // Continuous mode avoids keeping a static GLSurfaceView reference for requestRender.
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
}


