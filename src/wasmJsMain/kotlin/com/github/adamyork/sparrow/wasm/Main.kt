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
import com.github.adamyork.sparrow.wasm.gui.BodyElement
import com.github.adamyork.sparrow.wasm.gui.WasmBridgeColorScheme
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
fun main() {
    LogConfig.initialize(minimumLevel = Level.DEBUG)
    logger.info { "com.github.adamyork.sparrow.wasm.main called" }
    val component = AppConfig::class.create()
    val composeBodyMain = component.composeBodyMain
    val wasmBridgeColorScheme = component.wasmBridgeColorScheme
    buildGui(
        composeBodyMain = composeBodyMain,
        wasmBridgeColorScheme = wasmBridgeColorScheme
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun buildGui(
    composeBodyMain: BodyElement,
    wasmBridgeColorScheme: WasmBridgeColorScheme
) {
    ComposeViewport(
        viewportContainerId = "ComposeTarget"
    ) {
        MaterialTheme(
            colorScheme = wasmBridgeColorScheme.getScheme()
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
                        composeBodyMain.build()
                    }
                }
            }
        }
    }
}
