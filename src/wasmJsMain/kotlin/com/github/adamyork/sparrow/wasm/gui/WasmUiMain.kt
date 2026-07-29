package com.github.adamyork.sparrow.wasm.gui

import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.gui.UiDrawLayer
import com.github.adamyork.sparrow.platform.gui.UiMain
import com.github.adamyork.sparrow.platform.gui.QuitUi
import com.github.adamyork.sparrow.platform.gui.ScreenDimensionsService
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService
import androidx.compose.ui.unit.dp

class WasmUiMain(
    controller: UiController,
    runtimeService: RuntimeService,
    screenDimensionsService: ScreenDimensionsService,
    platformInterop: PlatformInterop,
    platformQuitUi: QuitUi
) : UiMain(controller, runtimeService, screenDimensionsService, platformInterop, platformQuitUi) {
    override var uiDrawLayer: UiDrawLayer = WasmJsUiDrawLayer(screenDimensionsService)
    override val centerHudWithinViewport: Boolean = true
    override val hudTopInset = 20.dp
    override val hudOverlayTopPadding = 0.dp
}
