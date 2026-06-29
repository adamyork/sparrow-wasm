package com.github.adamyork.sparrow.wasm.engine.data

import androidx.compose.ui.graphics.Color

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class Particle(
    val id: Int,
    val x: Int,
    val y: Int,
    val originX: Int,
    val originY: Int,
    val width: Int,
    val height: Int,
    val type: ParticleType,
    val frame: Int,
    val lifetime: Int,
    val xJitter: Int,
    val yJitter: Int,
    val radius: Int,
    val color: Color,
    val shape: ParticleShape
)
