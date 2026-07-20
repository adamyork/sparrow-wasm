package com.github.adamyork.sparrow.platform.gui.data
import androidx.compose.ui.graphics.Color
import com.github.adamyork.sparrow.platform.common.data.map.GameMapState
import com.github.adamyork.sparrow.platform.engine.data.DrawResult

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class UiState(
    val drawResult: DrawResult,
    val fpsLabel: String,
    val gameStatusLabel: String,
    val gameStatusLabelColor: Color,
    val scoreLabel: String,
    val totalLabel: String,
    val remainingLabel: String,
    val gameMapState: GameMapState,
    val completionTransitionRequested: Boolean
)
