package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.runtime.Composable
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.engine.Engine
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.ScoreService
import com.github.adamyork.sparrow.wasm.service.WavService
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class DefaultGame(
    private val assetService: AssetService,
    private val engine: Engine,
    private val particles: Particles,
    private val scoreService: ScoreService,
    private val statusProvider: StatusProvider,
    private val wavService: WavService
) : Game {

    private val controller = GameUiController(
        assetService = assetService,
        engine = engine,
        particles = particles,
        scoreService = scoreService,
        statusProvider = statusProvider,
        wavService = wavService
    )

    private val screen = GameUiMain(
        controller = controller
    )

    @Composable
    override fun build() {
        screen.build()
    }
}
