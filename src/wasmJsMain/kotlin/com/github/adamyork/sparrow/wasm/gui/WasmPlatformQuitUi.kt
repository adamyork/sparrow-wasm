package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.gui.PlatformQuitUi
import me.tatarka.inject.annotations.Inject


/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class WasmPlatformQuitUi : PlatformQuitUi {
    @Composable
    override fun BuildQuitButton(
        focusManager: FocusManager,
        disabledButtonColors: ButtonColors,
        textMainColor: Color
    ) {
    }
}

