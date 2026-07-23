package com.github.adamyork.sparrow.platform.gui

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface PlatformQuitUi {

    @Composable
    fun BuildQuitButton(
        focusManager: FocusManager,
        disabledButtonColors: ButtonColors,
        textMainColor: Color
    )

}

