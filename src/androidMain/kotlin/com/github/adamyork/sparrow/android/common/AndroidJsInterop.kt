package com.github.adamyork.sparrow.android.common

import com.github.adamyork.sparrow.platform.common.PlatformInterop

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
}