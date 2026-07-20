package com.github.adamyork.sparrow.android.common

import androidx.compose.runtime.Composable
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class AndroidJsInterop : PlatformInterop {
    override fun onReady(action: () -> Unit) {
        throw RuntimeException()
    }

    override fun getWindowHeight(): Double {
        throw RuntimeException()
    }

    override fun getWindowWidth(): Double {
        throw RuntimeException()
    }

    override fun hidePlatformLoader() {
        throw RuntimeException()
    }

    override fun getPlatformNowTime(): Double {
        throw RuntimeException()
    }

    override fun getBlobFromBytes(bytes: ByteArray): Any {
        throw RuntimeException()
    }

    override fun createAudioBlobUri(blob: Any): String {
        throw RuntimeException()
    }

    override fun isTouchDevice(): Boolean {
        throw RuntimeException()
    }

    override fun <T> addEventListener(type: String, callback: (T) -> Unit) {
        throw RuntimeException()
    }

    override fun <T> removeEventListener(type: String, callback: (T) -> Unit) {
        throw RuntimeException()
    }

    override fun requestAnimationFrame(callback: (Double) -> Unit): Int {
        throw RuntimeException()
    }

    override fun cancelAnimationFrame(handle: Int) {
        throw RuntimeException()
    }

    @Composable
    override fun InsertInputHandlers(
        controller: UiController,
        runtimeService: RuntimeService
    ) {
        throw RuntimeException()
    }
}