package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class UiScaffold {

    @OptIn(ExperimentalComposeUiApi::class)
    fun buildGui(
        game: Game,
        sparrowColorScheme: SparrowColorScheme
    ) {
        //TODO Interop
        ComposeViewport(
            viewportContainerId = "ComposeTarget"
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
