package com.github.adamyork.sparrow.wasm

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.github.adamyork.sparrow.platform.LogConfig
import com.github.adamyork.sparrow.wasm.gui.UiScaffold
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level

private val logger = KotlinLogging.logger {}

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    LogConfig.initialize(minimumLevel = Level.INFO)
    logger.info { "app started" }
    val component = AppConfig::class.create()
    component.platformInterop.onReady {
        val visibleHeight = component.platformInterop.getWindowHeight()
        val visibleWidth = component.platformInterop.getWindowWidth()
        component.screenDimensionsService.initialize(visibleWidth.toInt(), visibleHeight.toInt())
        logger.info { "screen dimensions: ${component.screenDimensionsService.getScreenDimensions()}" }
        component.platformInterop.hidePlatformLoader()
        val gameLayer = component.game
        val sparrowColorScheme = component.sparrowColorScheme
        ComposeViewport(viewportContainerId = "ComposeTarget") {
            UiScaffold().buildGui(gameLayer, sparrowColorScheme)
        }
    }
}
