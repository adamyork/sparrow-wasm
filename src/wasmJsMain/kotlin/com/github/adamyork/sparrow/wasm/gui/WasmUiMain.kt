package com.github.adamyork.sparrow.wasm.gui

import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.gui.PlatformUiDrawLayer
import com.github.adamyork.sparrow.platform.gui.PlatformUiMain
import com.github.adamyork.sparrow.platform.gui.ScreenDimensionsService
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService

class WasmUiMain(
    controller: UiController,
    runtimeService: RuntimeService,
    screenDimensionsService: ScreenDimensionsService,
    platformInterop: PlatformInterop
) : PlatformUiMain(controller, runtimeService, screenDimensionsService, platformInterop) {
    override var uiDrawLayer: PlatformUiDrawLayer = WasmJsUiDrawLayer(screenDimensionsService)
}