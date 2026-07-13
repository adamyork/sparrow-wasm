package com.github.adamyork.sparrow.wasm

import com.github.adamyork.sparrow.wasm.gui.GameUiScaffold
import com.github.adamyork.sparrow.wasm.gui.data.ScreenDimensions
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.skiko.wasm.onWasmReady
import org.w3c.dom.HTMLElement

private val logger = KotlinLogging.logger {}

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
fun main() {
    LogConfig.initialize(minimumLevel = Level.INFO)
    logger.info { "app started" }
    onWasmReady {
        logger.info { "WASM environment is ready. Building GUI" }
        val viewport = getVisualViewport()
        val visibleHeight = viewport.height
        val screenDimensions = ScreenDimensions.fromScreenResolution(
            window.innerWidth,
            visibleHeight.toInt()
        )
        logger.info { "screen dimensions: $screenDimensions" }
        document.getElementById("loading-screen")?.let {
            (it as HTMLElement).style.display = "none"
        }
        val component = AppConfig::class.create()
        val gameLayer = component.game
        val sparrowColorScheme = component.sparrowColorScheme
        val gameUIScaffold = GameUiScaffold()
        gameUIScaffold.buildGui(
            game = gameLayer,
            sparrowColorScheme = sparrowColorScheme,
            screenDimensions = screenDimensions
        )
    }
}
