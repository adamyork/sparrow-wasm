package com.github.adamyork.sparrow.wasm.common

import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.wasm.AppScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.document
import kotlinx.browser.window
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skiko.wasm.onWasmReady
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob

@AppScope
@Inject
class WasmJsInterop : PlatformInterop {

    private val logger = KotlinLogging.logger {}

    override fun onReady(action: () -> Unit) {
        onWasmReady {
            logger.info { "WASM environment is ready. Building GUI" }
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

    override fun getBlobFromInt8Array(int8Array: Int8Array): Any {
        return createBlobFromInt8Array(int8Array)
    }

    override fun createAudioBlobUri(blob: Any): String {
        return URL.createObjectURL(blob as Blob)
    }

}
