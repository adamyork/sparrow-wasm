package com.github.adamyork.sparrow.platform.common

import androidx.compose.runtime.Composable
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
object NullPlatformInterop : PlatformInterop {

    override fun onReady(action: () -> Unit) = Unit

    override fun getWindowHeight(): Double = 0.0

    override fun getWindowWidth(): Double = 0.0

    override fun hidePlatformLoader() = Unit

    override fun getPlatformNowTime(): Double = 0.0

    override fun getBlobFromBytes(bytes: ByteArray): Any = unsupported("Blob interop")

    override fun createAudioBlobUri(blob: Any): String = unsupported("Audio URI interop")

    override fun isTouchDevice(): Boolean = unsupported("Touch interop")

    override fun <T> addEventListener(type: String, callback: (T) -> Unit) {
        unsupported<Unit>("Event interop")
    }

    override fun <T> removeEventListener(type: String, callback: (T) -> Unit) {
        unsupported<Unit>("Event interop")
    }

    override fun requestAnimationFrame(callback: (Double) -> Unit): Int = unsupported("Animation frame interop")

    override fun cancelAnimationFrame(handle: Int) {
        unsupported<Unit>("Animation frame interop")
    }

    @Composable
    override fun InsertInputHandlers(controller: UiController, runtimeService: RuntimeService) {
        unsupported<Unit>("Input handler interop")
    }

    private fun <T> unsupported(label: String): T {
        throw UnsupportedOperationException("$label is not available for placeholder player")
    }
}

