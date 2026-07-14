package com.github.adamyork.sparrow.wasm.gui

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.gui.data.ScreenDimensions
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultScreenDimensionsService : ScreenDimensionsService {

    private var screenDimensions: ScreenDimensions? = null

    override fun initialize(screenWidth: Int, screenHeight: Int) {
        screenDimensions = ScreenDimensions.fromScreenResolution(screenWidth, screenHeight)
    }

    override fun getScreenDimensions(): ScreenDimensions {
        return screenDimensions ?: error("ScreenDimensions has not been initialized")
    }
}

