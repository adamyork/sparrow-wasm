package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import com.github.adamyork.sparrow.wasm.gui.data.LocalScreenDimensions
import com.github.adamyork.sparrow.wasm.gui.data.ScreenDimensions

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class GameUiScaffold {

    @OptIn(ExperimentalComposeUiApi::class)
    fun buildGui(
        game: Game,
        sparrowColorScheme: SparrowColorScheme,
        screenDimensions: ScreenDimensions
    ) {
        ComposeViewport(
            viewportContainerId = "ComposeTarget"
        ) {
            CompositionLocalProvider(
                LocalScreenDimensions provides screenDimensions
            ) {
                MaterialTheme(
                    colorScheme = sparrowColorScheme.getScheme(),
                    typography = sparrowColorScheme.getTypography()
                ) {
                    Scaffold(
                        modifier = Modifier
                            .semantics { contentDescription = "Application scaffold" }
                            .testTag("app-scaffold"),
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .semantics {
                                    contentDescription = "Main page layout container"
                                }
                                .testTag("main-layout")
                                .padding(innerPadding),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 1200.dp)
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription =
                                            "Main content max width container"
                                    }
                                    .testTag("main-content-wrapper")
                            ) {
                                game.build()
                            }
                        }
                    }
                }
            }
        }
    }
}
