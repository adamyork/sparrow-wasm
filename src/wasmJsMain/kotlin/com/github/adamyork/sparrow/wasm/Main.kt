package com.github.adamyork.sparrow.wasm

import com.github.adamyork.sparrow.wasm.gui.GameUIScaffold
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level

private val logger = KotlinLogging.logger {}

fun main() {
    LogConfig.initialize(minimumLevel = Level.INFO)
    logger.info { "app started" }
    val component = AppConfig::class.create()
    val gameLayer = component.game
    val sparrowColorScheme = component.sparrowColorScheme
    val gameUIScaffold = GameUIScaffold()
    gameUIScaffold.buildGui(
        game = gameLayer,
        sparrowColorScheme = sparrowColorScheme
    )
}
