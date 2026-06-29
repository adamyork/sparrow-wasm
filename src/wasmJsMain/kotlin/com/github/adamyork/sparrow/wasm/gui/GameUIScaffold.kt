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

class GameUIScaffold {

    @OptIn(ExperimentalComposeUiApi::class)
    fun buildGui(
        game: Game,
        sparrowColorScheme: SparrowColorScheme
    ) {
        val theme = sparrowColorScheme as? DefaultSparrowColorScheme
            ?: throw IllegalStateException("Expected DefaultSparrowColorScheme")
        ComposeViewport(
            viewportContainerId = "ComposeTarget"
        ) {
            MaterialTheme(
                colorScheme = theme.getScheme() // Uses your CSS-overridden scheme
            ) {
                Scaffold(
                    modifier = Modifier
                        .semantics { contentDescription = "Application scaffold" }
                        .testTag("app-scaffold"),
                    // Apply the theme background color to the scaffold
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background) // Explicit background
                            .semantics {
                                contentDescription = "com.github.adamyork.sparrow.wasm.Main page layout container"
                            }
                            .testTag("app-com.github.adamyork.sparrow.wasm.main-layout")
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 1200.dp)
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription =
                                        "com.github.adamyork.sparrow.wasm.Main content max width container"
                                }
                                .testTag("app-com.github.adamyork.sparrow.wasm.main-content-wrapper")
                        ) {
                            game.build()
                        }
                    }
                }
            }
        }
    }
}