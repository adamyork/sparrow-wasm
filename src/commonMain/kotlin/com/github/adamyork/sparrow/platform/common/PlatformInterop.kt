package com.github.adamyork.sparrow.platform.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.github.adamyork.sparrow.platform.common.data.ControlAction
import com.github.adamyork.sparrow.platform.common.data.ControlType
import com.github.adamyork.sparrow.platform.common.data.LifeCycleState
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService
import kotlinx.coroutines.awaitCancellation

interface PlatformInterop {

    fun onReady(action: () -> Unit)

    fun getWindowHeight(): Double

    fun getWindowWidth(): Double

    fun hidePlatformLoader()

    fun getPlatformNowTime(): Double

    fun getBlobFromBytes(bytes: ByteArray): Any

    fun createAudioBlobUri(blob: Any): String

    fun isTouchDevice() : Boolean

    fun <T> addEventListener(type: String, callback: (T) -> Unit)

    fun <T> removeEventListener(type: String, callback: (T) -> Unit)

    fun requestAnimationFrame(callback: (Double) -> Unit): Int

    fun cancelAnimationFrame(handle: Int)

    @Composable
    fun InsertInputHandlers(controller: UiController, runtimeService: RuntimeService)

}
