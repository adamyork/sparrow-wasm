package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

interface SparrowColorScheme {

    @Composable
    fun getScheme(): ColorScheme

    @Composable
    fun getHoverColor(): Color

    @Composable
    fun getFocusOutlineColor(): Color

    @Composable
    fun getDisabledOpacity(): Float

}
