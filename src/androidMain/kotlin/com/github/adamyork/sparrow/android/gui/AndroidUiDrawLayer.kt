package com.github.adamyork.sparrow.android.gui // adjust to your android package structure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.core.graphics.scale
import com.github.adamyork.sparrow.android.engine.data.AndroidPlatformImage
import com.github.adamyork.sparrow.platform.engine.data.PlatformImage
import com.github.adamyork.sparrow.platform.gui.PlatformUiDrawLayer
import com.github.adamyork.sparrow.platform.gui.ScreenDimensionsService

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class AndroidUiDrawLayer(screenDimensionsService: ScreenDimensionsService) : PlatformUiDrawLayer(
    screenDimensionsService
) {

    override var foregroundPaint: Any = Paint().apply {}

    private var splashImageBitmap: ImageBitmap? by mutableStateOf(null)
    private var farGroundBitmap: ImageBitmap? by mutableStateOf(null)
    private var farGroundOffsetX: Float by mutableFloatStateOf(0f)
    private var farGroundOffsetY: Float by mutableFloatStateOf(0f)
    private var midGroundBitmap: ImageBitmap? by mutableStateOf(null)
    private var midGroundOffsetX: Float by mutableFloatStateOf(0f)
    private var midGroundOffsetY: Float by mutableFloatStateOf(0f)
    private var collisionBitmap: ImageBitmap? by mutableStateOf(null)
    private var collisionOffsetX: Float by mutableFloatStateOf(0f)
    private var collisionOffsetY: Float by mutableFloatStateOf(0f)
    override var foregroundBitmap: Any? by mutableStateOf(null)
    private var nearFieldBitmap: ImageBitmap? by mutableStateOf(null)
    private var nearFieldOffsetX: Float by mutableFloatStateOf(0f)
    private var nearFieldOffsetY: Float by mutableFloatStateOf(0f)

    @Composable
    override fun ForegroundLayerCanvas(image: Any?) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
        ) {
            image?.let { foreground ->
                val imageBitmap = when (foreground) {
                    is android.graphics.Bitmap -> foreground.asImageBitmap()
                    is ImageBitmap -> foreground
                    else -> return@let
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawBitmap(
                        ((foreground as? android.graphics.Bitmap) ?: return@drawIntoCanvas).scale(
                            (imageBitmap.width * density).toInt(),
                            (imageBitmap.height * density).toInt()
                        ),
                        0f,
                        0f,
                        null
                    )
                }
            }
        }
    }

    override fun drawForeground(image: PlatformImage) {
        foregroundBitmap = (image as AndroidPlatformImage).bitmap
    }

    override fun clearAllLayers() {
        splashImageBitmap = null
        farGroundBitmap = null
        midGroundBitmap = null
        collisionBitmap = null
        nearFieldBitmap = null
        farGroundOffsetX = 0f
        farGroundOffsetY = 0f
        midGroundOffsetX = 0f
        midGroundOffsetY = 0f
        collisionOffsetX = 0f
        collisionOffsetY = 0f
        nearFieldOffsetX = 0f
        nearFieldOffsetY = 0f
        foregroundBitmap = null
    }
}