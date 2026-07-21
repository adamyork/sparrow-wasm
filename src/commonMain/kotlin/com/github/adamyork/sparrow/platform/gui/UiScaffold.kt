package com.github.adamyork.sparrow.platform.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class UiScaffold {

    @Composable
    fun BuildGui(
        game: PlatformGame,
        sparrowColorScheme: SparrowColorScheme
    ) {
        MaterialTheme(
            colorScheme = sparrowColorScheme.getScheme(),
            typography = sparrowColorScheme.getTypography()
        ) {
            Scaffold(
                modifier = Modifier
                    .semantics { contentDescription = "Application scaffold" }
                    .testTag("app-scaffold"),
                contentWindowInsets = WindowInsets(0.dp),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .semantics { contentDescription = "Main page layout container" }
                        .testTag("main-layout"),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 1200.dp)
                            .fillMaxWidth()
                            .semantics { contentDescription = "Main content max width container" }
                            .testTag("main-content-wrapper")
                    ) {
                        game.Build()
                    }
                }
            }
        }
    }
}
