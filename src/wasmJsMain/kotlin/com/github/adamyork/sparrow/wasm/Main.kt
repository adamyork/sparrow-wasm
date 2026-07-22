package com.github.adamyork.sparrow.wasm

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.github.adamyork.sparrow.platform.LogConfig
import com.github.adamyork.sparrow.platform.gui.UiScaffold
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

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
        component.screenDimensionsService.initialize(
            component.platformInterop.getWindowWidth().toInt(),
            component.platformInterop.getWindowHeight().toInt()
        )
        (document.getElementById("ComposeTarget") as? HTMLElement)?.style?.apply {
            width = "${component.platformInterop.getWindowWidth().toInt()}px"
            height = "${component.platformInterop.getWindowHeight().toInt()}px"
            minWidth = "${component.platformInterop.getWindowWidth().toInt()}px"
            minHeight = "${component.platformInterop.getWindowHeight().toInt()}px"
            maxWidth = "${component.platformInterop.getWindowWidth().toInt()}px"
            maxHeight = "${component.platformInterop.getWindowHeight().toInt()}px"
        }
        logger.info { "screen dimensions: ${component.screenDimensionsService.getScreenDimensions()}" }
        component.platformInterop.hidePlatformLoader()
        val gameLayer = component.game
        val sparrowColorScheme = component.sparrowColorScheme
        ComposeViewport(viewportContainerId = "ComposeTarget") {
            UiScaffold().BuildGui(gameLayer, sparrowColorScheme)
        }
    }
}
