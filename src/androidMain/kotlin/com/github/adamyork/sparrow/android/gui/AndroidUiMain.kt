package com.github.adamyork.sparrow.android.gui

import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.gui.UiDrawLayer
import com.github.adamyork.sparrow.platform.gui.UiMain
import com.github.adamyork.sparrow.platform.gui.QuitUi
import com.github.adamyork.sparrow.platform.gui.ScreenDimensionsService
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService

class AndroidUiMain(
    controller: UiController,
    runtimeService: RuntimeService,
    screenDimensionsService: ScreenDimensionsService,
    platformInterop: PlatformInterop,
    platformQuitUi: QuitUi
) : UiMain(controller, runtimeService, screenDimensionsService, platformInterop, platformQuitUi) {
    override var uiDrawLayer: UiDrawLayer = AndroidUiDrawLayer(screenDimensionsService)
}
