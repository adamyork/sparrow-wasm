package com.github.adamyork.sparrow.wasm.gui

import com.github.adamyork.sparrow.wasm.gui.data.ScreenDimensions

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface ScreenDimensionsService {

    fun initialize(screenWidth: Int, screenHeight: Int)

    fun getScreenDimensions(): ScreenDimensions
}

