package com.github.adamyork.sparrow.android.common

import android.content.res.Configuration
import android.content.res.Resources
import android.os.SystemClock
import android.util.Base64
import android.view.Choreographer
import androidx.compose.runtime.Composable
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService
import me.tatarka.inject.annotations.Inject
import java.util.concurrent.atomic.AtomicInteger

@AppScope
@Inject
class AndroidJsInterop : PlatformInterop {
    private val frameIdCounter = AtomicInteger(1)
    private val frameCallbacks = mutableMapOf<Int, Choreographer.FrameCallback>()
    private val listenersByType = mutableMapOf<String, MutableSet<(Any) -> Unit>>()
    private val callbackAdaptersByType = mutableMapOf<String, MutableMap<Any, (Any) -> Unit>>()

    @Volatile
    private var lastFrameTimeMs: Double = 0.0

    override fun onReady(action: () -> Unit) {
        action()
    }

    override fun getWindowHeight(): Double {
        return Resources.getSystem().displayMetrics.heightPixels.toDouble()
    }

    override fun getWindowWidth(): Double {
        return Resources.getSystem().displayMetrics.widthPixels.toDouble()
    }

    override fun hidePlatformLoader() {
        // No Android platform loader to hide.
    }

    override fun getPlatformNowTime(): Double {
        return if (lastFrameTimeMs > 0.0) {
            lastFrameTimeMs
        } else {
            SystemClock.elapsedRealtimeNanos() / 1_000_000.0
        }
    }

    override fun getBlobFromBytes(bytes: ByteArray): Any {
        return bytes
    }

    override fun createAudioBlobUri(blob: Any): String {
        val bytes = blob as? ByteArray
            ?: throw IllegalArgumentException("Expected ByteArray for Android audio blob")
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:audio/wav;base64,$encoded"
    }

    override fun isTouchDevice(): Boolean {
        return Resources.getSystem().configuration.touchscreen != Configuration.TOUCHSCREEN_NOTOUCH
    }

    override fun <T> addEventListener(type: String, callback: (T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val callbackKey = callback as Any
        val adaptedCallback: (Any) -> Unit = { event ->
            @Suppress("UNCHECKED_CAST")
            callback(event as T)
        }
        synchronized(listenersByType) {
            callbackAdaptersByType.getOrPut(type) { mutableMapOf() }[callbackKey] = adaptedCallback
            listenersByType.getOrPut(type) { mutableSetOf() }.add(adaptedCallback)
        }
    }

    override fun <T> removeEventListener(type: String, callback: (T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val callbackKey = callback as Any
        synchronized(listenersByType) {
            val adapter = callbackAdaptersByType[type]?.remove(callbackKey)
            if (adapter != null) {
                listenersByType[type]?.remove(adapter)
            }
            if (listenersByType[type].isNullOrEmpty()) {
                listenersByType.remove(type)
                callbackAdaptersByType.remove(type)
            }
        }
    }

    override fun requestAnimationFrame(callback: (Double) -> Unit): Int {
        val frameId = frameIdCounter.getAndIncrement()
        val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
            val frameTimeMs = frameTimeNanos / 1_000_000.0
            lastFrameTimeMs = frameTimeMs
            callback(frameTimeMs)
        }
        synchronized(frameCallbacks) {
            frameCallbacks[frameId] = frameCallback
        }
        Choreographer.getInstance().postFrameCallback(frameCallback)
        return frameId
    }

    override fun cancelAnimationFrame(handle: Int) {
        val callback = synchronized(frameCallbacks) { frameCallbacks.remove(handle) } ?: return
        Choreographer.getInstance().removeFrameCallback(callback)
    }

    @Composable
    override fun InsertInputHandlers(
        controller: UiController,
        runtimeService: RuntimeService
    ) {
        // Android gameplay input is touch-only.
    }
}
