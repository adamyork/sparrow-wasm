package com.github.adamyork.sparrow.wasm.gui

import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.gui.PlatformUiDrawLayer
import com.github.adamyork.sparrow.platform.gui.PlatformUiMain
import com.github.adamyork.sparrow.platform.gui.ScreenDimensionsService
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService
import androidx.compose.ui.unit.dp

class WasmUiMain(
    controller: UiController,
    runtimeService: RuntimeService,
    screenDimensionsService: ScreenDimensionsService,
    platformInterop: PlatformInterop
) : PlatformUiMain(controller, runtimeService, screenDimensionsService, platformInterop) {
    override var uiDrawLayer: PlatformUiDrawLayer = WasmJsUiDrawLayer(screenDimensionsService)
    override val centerHudWithinViewport: Boolean = true
    override val hudTopInset = 20.dp
    override val hudOverlayTopPadding = 0.dp
}
