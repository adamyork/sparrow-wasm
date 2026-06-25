package com.github.adamyork.sparrow.wasm

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.skia.Surface

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
)
