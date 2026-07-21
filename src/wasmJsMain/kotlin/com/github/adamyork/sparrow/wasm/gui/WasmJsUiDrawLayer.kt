package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import com.github.adamyork.sparrow.platform.engine.data.PlatformImage
import com.github.adamyork.sparrow.platform.gui.PlatformUiDrawLayer
import com.github.adamyork.sparrow.platform.gui.ScreenDimensionsService
import com.github.adamyork.sparrow.wasm.engine.data.WasmJsPlatformImage
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Image as SkiaImage

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class WasmJsUiDrawLayer(screenDimensionsService: ScreenDimensionsService) : PlatformUiDrawLayer(
    screenDimensionsService
) {

    override var foregroundPaint: Any = Paint().apply {
        isAntiAlias = true
    }

    private var splashImageBitmap: ImageBitmap? by mutableStateOf(null)
    private var farGroundBitmap: ImageBitmap? by mutableStateOf(null)
    private var farGroundOffsetX: Float by mutableStateOf(0f)
    private var farGroundOffsetY: Float by mutableStateOf(0f)
    private var midGroundBitmap: ImageBitmap? by mutableStateOf(null)
    private var midGroundOffsetX: Float by mutableStateOf(0f)
    private var midGroundOffsetY: Float by mutableStateOf(0f)
    private var collisionBitmap: ImageBitmap? by mutableStateOf(null)
    private var collisionOffsetX: Float by mutableStateOf(0f)
    private var collisionOffsetY: Float by mutableStateOf(0f)
    override var foregroundBitmap: Any? by mutableStateOf(null)
    private var nearFieldBitmap: ImageBitmap? by mutableStateOf(null)
    private var nearFieldOffsetX: Float by mutableStateOf(0f)
    private var nearFieldOffsetY: Float by mutableStateOf(0f)

    @Composable
    override fun ForegroundLayerCanvas(image: Any?) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .clip(RectangleShape)
        ) {
            image?.let { foreground ->
                drawIntoCanvas { canvas ->
                    canvas.skiaCanvas.drawImageRect(
                        image = foreground as SkiaImage,
                        src = Rect.makeXYWH(0f, 0f, foreground.width.toFloat(), foreground.height.toFloat()),
                        dst = Rect.makeXYWH(
                            0f,
                            0f,
                            foreground.width.toFloat() * density,
                            foreground.height.toFloat() * density
                        ),
                        samplingMode = SamplingMode.LINEAR,
                        paint = foregroundPaint as Paint,
                        strict = true
                    )
                }
            }
        }
    }

    override fun drawForeground(image: PlatformImage) {
        (foregroundBitmap as SkiaImage?)?.close()
        foregroundBitmap = (image as WasmJsPlatformImage).image
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
        (foregroundBitmap as SkiaImage?)?.close()
        foregroundBitmap = null
    }

}
