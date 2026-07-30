package com.github.adamyork.sparrow.wasm.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.ControlAction
import com.github.adamyork.sparrow.platform.common.data.ControlType
import com.github.adamyork.sparrow.platform.common.data.LifeCycleState
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.awaitCancellation
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skiko.wasm.onWasmReady
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.url.URL
import org.w3c.files.Blob

@AppScope
@Inject
class WasmJsInterop : PlatformInterop {

    private val logger = KotlinLogging.logger {}

    private val eventListenerWrappers = mutableMapOf<Pair<String, Any>, (Event) -> Unit>()

    override fun onReady(action: () -> Unit) {
        onWasmReady {
            action()
        }
    }

    override fun getWindowHeight(): Double {
        return getVisualViewport().height
    }

    override fun getWindowWidth(): Double {
        return getVisualViewport().width
    }

    override fun hidePlatformLoader() {
        document.getElementById("loading-screen")?.let {
            (it as HTMLElement).style.display = "none"
        }
    }

    override fun getPlatformNowTime(): Double {
        return window.performance.now()
    }

    override fun getBlobFromBytes(bytes: ByteArray): Any {
        return createBlobFromInt8Array(bytes.toInt8Array())
    }

    override fun createAudioBlobUri(blob: Any): String {
        return URL.createObjectURL(blob as Blob)
    }

    override fun isTouchDevice(): Boolean {
        return window.navigator.maxTouchPoints > 0
    }


    override fun <T> addEventListener(type: String, callback: (T) -> Unit) {
        logger.debug { "add event listener $type" }
        val callbackKey = callback as Any
        val listenerKey = type to callbackKey
        val nativeCallback = eventListenerWrappers.getOrPut(listenerKey) {
            { event ->
                @Suppress("UNCHECKED_CAST")
                callback(event as T)
            }
        }
        window.addEventListener(type, nativeCallback)
    }

    override fun <T> removeEventListener(type: String, callback: (T) -> Unit) {
        logger.debug { "remove event listener $type" }
        val callbackKey = callback as Any
        val listenerKey = type to callbackKey
        val nativeCallback = eventListenerWrappers.remove(listenerKey)
        if (nativeCallback != null) {
            window.removeEventListener(type, nativeCallback)
        }
    }

    override fun requestAnimationFrame(callback: (Double) -> Unit): Int {
        return window.requestAnimationFrame { timestamp ->
            callback(timestamp)
        }
    }

    override fun cancelAnimationFrame(handle: Int) {
        window.cancelAnimationFrame(handle)
    }

    @Composable
    override fun InsertInputHandlers(
        controller: UiController,
        runtimeService: RuntimeService
    ) {
        return LaunchedEffect(Unit) {
            val activeKeys = mutableSetOf<String>()

            fun evaluateInputs() {
                if (runtimeService.lifeCycleState == LifeCycleState.RUNNING) {
                    val moveLeft = activeKeys.contains("arrowleft")
                    val moveRight = activeKeys.contains("arrowright")
                    val doJump = activeKeys.contains("space") || activeKeys.contains(" ") || activeKeys.contains("spacebar")

                    controller.applyInput(if (moveLeft) ControlType.START else ControlType.STOP, ControlAction.LEFT)
                    controller.applyInput(if (moveRight) ControlType.START else ControlType.STOP, ControlAction.RIGHT)
                    controller.applyInput(if (doJump) ControlType.START else ControlType.STOP, ControlAction.JUMP)
                }
            }

            val keyDownListener: (Event) -> Unit = { event ->
                if (event is KeyboardEvent) {
                    val code = event.code.lowercase()
                    val key = event.key.lowercase()
                    if (code == "arrowleft" || code == "arrowright" || code == "space" || key == " ") {
                        event.preventDefault()
                    }
                    activeKeys.add(code)
                    activeKeys.add(key)
                    evaluateInputs()
                }
            }

            val keyUpListener: (Event) -> Unit = { event ->
                if (event is KeyboardEvent) {
                    val code = event.code.lowercase()
                    val key = event.key.lowercase()
                    activeKeys.remove(code)
                    activeKeys.remove(key)
                    evaluateInputs()
                }
            }

            val resetAll: (Event) -> Unit = {
                activeKeys.clear()
                controller.applyInput(ControlType.STOP, ControlAction.LEFT)
                controller.applyInput(ControlType.STOP, ControlAction.RIGHT)
                controller.applyInput(ControlType.STOP, ControlAction.JUMP)
            }

            // Hooking into your existing global wrapper mechanism
            addEventListener("keydown", keyDownListener)
            addEventListener("keyup", keyUpListener)
            addEventListener("blur", resetAll)
            addEventListener("visibilitychange", resetAll)

            try {
                awaitCancellation()
            } finally {
                removeEventListener("keydown", keyDownListener)
                removeEventListener("keyup", keyUpListener)
                removeEventListener("blur", resetAll)
                removeEventListener("visibilitychange", resetAll)
            }
        }
    }

}
