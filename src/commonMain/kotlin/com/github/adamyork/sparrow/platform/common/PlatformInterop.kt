package com.github.adamyork.sparrow.platform.common

import org.khronos.webgl.Int8Array

interface PlatformInterop {

    fun onReady(action: () -> Unit)

    fun getWindowHeight(): Double

    fun getWindowWidth(): Double

    fun hidePlatformLoader()

    fun getBlobFromInt8Array(int8Array: Int8Array): Any

}