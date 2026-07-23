package com.github.adamyork.sparrow.android.gui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.gui.PlatformQuitUi
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class AndroidPlatformQuitUi : PlatformQuitUi {
    @Composable
    override fun BuildQuitButton(
        focusManager: FocusManager,
        disabledButtonColors: ButtonColors,
        textMainColor: Color
    ) {
        val context = LocalContext.current

        Button(
            onClick = {
                focusManager.clearFocus(force = true)
                context.findActivity()?.finishAndRemoveTask()
            },
            colors = disabledButtonColors,
            modifier = Modifier
                .focusProperties { canFocus = false }
                .semantics { contentDescription = "quit-button" }
                .testTag("quit-button")
        ) {
            Text("Quit", color = textMainColor)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

