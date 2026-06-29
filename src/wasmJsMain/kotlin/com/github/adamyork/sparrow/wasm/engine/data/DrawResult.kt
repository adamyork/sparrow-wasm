package com.github.adamyork.sparrow.wasm.engine.data

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.skia.Surface

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class DrawResult(
    val foregroundSurface: Surface?,
    val farGroundBitmap: ImageBitmap?,
    val farGroundOffsetX: Float,
    val farGroundOffsetY: Float,
    val midGroundBitmap: ImageBitmap?,
    val midGroundOffsetX: Float,
    val midGroundOffsetY: Float,
    val collisionBitmap: ImageBitmap?,
    val collisionOffsetX: Float,
    val collisionOffsetY: Float,
    val foregroundOffsetX: Float,
    val foregroundOffsetY: Float,
) {
    companion object {
        val EMPTY_DRAW_RESULT = DrawResult(
            null,
            null,
            0f,
            0f,
            null,
            0f,
            0f,
            null,
            0f,
            0f,
            0f,
            0f
        )
    }
}