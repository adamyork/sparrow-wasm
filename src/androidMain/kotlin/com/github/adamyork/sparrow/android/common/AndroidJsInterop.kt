package com.github.adamyork.sparrow.android.common

import android.content.res.Configuration
import android.content.res.Resources
import android.os.SystemClock
import android.util.Base64
import android.view.Choreographer
import android.view.KeyEvent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.ControlAction
import com.github.adamyork.sparrow.platform.common.data.ControlType
import com.github.adamyork.sparrow.platform.common.data.LifeCycleState
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService
import kotlinx.coroutines.awaitCancellation
import me.tatarka.inject.annotations.Inject
import java.util.concurrent.atomic.AtomicInteger

@AppScope
@Inject
class AndroidJsInterop : PlatformInterop {
    private val frameIdCounter = AtomicInteger(1)
    private val frameCallbacks = mutableMapOf<Int, Choreographer.FrameCallback>()
    private val listenersByType = mutableMapOf<String, MutableSet<(Any) -> Unit>>()
    private val callbackAdaptersByType = mutableMapOf<String, MutableMap<Any, (Any) -> Unit>>()

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
        return SystemClock.elapsedRealtimeNanos() / 1_000_000.0
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
            callback(frameTimeNanos / 1_000_000.0)
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
        val view = LocalView.current
        LaunchedEffect(view) {
            fun onKeyDown(event: KeyEvent) {
                if (runtimeService.lifeCycleState == LifeCycleState.RUNNING) {
                    val action = toControlAction(event)
                    if (action != null) {
                        controller.applyInput(ControlType.START, action)
                    }
                }
            }

            fun onKeyUp(event: KeyEvent) {
                if (runtimeService.lifeCycleState == LifeCycleState.RUNNING) {
                    val action = toControlAction(event)
                    if (action != null) {
                        controller.applyInput(ControlType.STOP, action)
                    }
                }
            }

            val listener = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        dispatchEvent("keydown", event)
                        runtimeService.lifeCycleState == LifeCycleState.RUNNING && toControlAction(event) != null
                    }

                    KeyEvent.ACTION_UP -> {
                        dispatchEvent("keyup", event)
                        runtimeService.lifeCycleState == LifeCycleState.RUNNING && toControlAction(event) != null
                    }

                    else -> false
                }
            }

            addEventListener("keydown", ::onKeyDown)
            addEventListener("keyup", ::onKeyUp)
            view.isFocusableInTouchMode = true
            view.requestFocus()
            ViewCompat.addOnUnhandledKeyEventListener(view, listener)
            try {
                awaitCancellation()
            } finally {
                removeEventListener("keydown", ::onKeyDown)
                removeEventListener("keyup", ::onKeyUp)
                ViewCompat.removeOnUnhandledKeyEventListener(view, listener)
            }
        }
    }

    private fun toControlAction(event: KeyEvent): ControlAction? {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> ControlAction.LEFT

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> ControlAction.RIGHT

            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> ControlAction.JUMP

            else -> null
        }
    }

    private fun dispatchEvent(type: String, event: Any) {
        val callbacks = synchronized(listenersByType) { listenersByType[type]?.toList().orEmpty() }
        callbacks.forEach { callback -> callback(event) }
    }
}
