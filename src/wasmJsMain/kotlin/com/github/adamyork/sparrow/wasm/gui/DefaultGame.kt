package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.runtime.Composable
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.service.WavService
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.engine.Engine
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.RuntimeService
import com.github.adamyork.sparrow.wasm.service.ScoreService
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultGame(
    private val assetService: AssetService,
    private val engine: Engine,
    private val particles: Particles,
    private val scoreService: ScoreService,
    private val runtimeService: RuntimeService,
    private val wavService: WavService,
    private val screenDimensionsService: ScreenDimensionsService,
    private val platformInterop: PlatformInterop
) : Game {

    private val controller = UiController(
        assetService = assetService,
        engine = engine,
        particles = particles,
        scoreService = scoreService,
        runtimeService = runtimeService,
        wavService = wavService,
        screenDimensionsService = screenDimensionsService
    )

    private val screen = UiMain(
        controller = controller,
        runtimeService = runtimeService,
        screenDimensionsService = screenDimensionsService,
        platformInterop = platformInterop
    )

    @Composable
    override fun build() {
        screen.build()
    }
}
