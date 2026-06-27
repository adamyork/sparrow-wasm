package com.github.adamyork.sparrow.wasm

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.github.adamyork.sparrow.wasm.gui.GameLayer
import com.github.adamyork.sparrow.wasm.gui.SparrowColorScheme
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
fun main() {
    LogConfig.initialize(minimumLevel = Level.INFO)
    logger.info { "app started" }
    val component = AppConfig::class.create()
    val gameLayer = component.gameLayer
    val sparrowColorScheme = component.sparrowColorScheme
    buildGui(
        gameLayer = gameLayer,
        sparrowColorScheme = sparrowColorScheme
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun buildGui(
    gameLayer: GameLayer,
    sparrowColorScheme: SparrowColorScheme
) {
    ComposeViewport(
        viewportContainerId = "ComposeTarget"
    ) {
        MaterialTheme(
            colorScheme = sparrowColorScheme.getScheme()
        ) {
            Scaffold(
                modifier = Modifier
                    .semantics { contentDescription = "Application scaffold" }
                    .testTag("app-scaffold"),
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                                contentDescription = "com.github.adamyork.sparrow.wasm.Main content max width container"
                            }
                            .testTag("app-com.github.adamyork.sparrow.wasm.main-content-wrapper")
                    ) {
                        gameLayer.build()
                    }
                }
            }
        }
    }
}
