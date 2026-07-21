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
        val visibleHeight = component.platformInterop.getWindowHeight()
        val visibleWidth = component.platformInterop.getWindowWidth()
        val lockedWidth = visibleWidth.toInt()
        val lockedHeight = visibleHeight.toInt()
        component.screenDimensionsService.initialize(lockedWidth, lockedHeight)
        (document.getElementById("ComposeTarget") as? HTMLElement)?.style?.apply {
            width = "${lockedWidth}px"
            height = "${lockedHeight}px"
            minWidth = "${lockedWidth}px"
            minHeight = "${lockedHeight}px"
            maxWidth = "${lockedWidth}px"
            maxHeight = "${lockedHeight}px"
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
