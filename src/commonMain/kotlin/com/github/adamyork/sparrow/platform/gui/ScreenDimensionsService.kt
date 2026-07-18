package com.github.adamyork.sparrow.platform.gui

import com.github.adamyork.sparrow.platform.gui.data.ScreenDimensions

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface ScreenDimensionsService {

    fun initialize(screenWidth: Int, screenHeight: Int)

    fun getScreenDimensions(): ScreenDimensions
}

