package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.github.adamyork.sparrow.wasm.gui.data.ScreenDimensions
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Image as SkiaImage

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class UiDrawLayer(
    private val screenDimensionsService: ScreenDimensionsService
) {

    private val foregroundPaint = Paint().apply {
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
    private var foregroundBitmap: SkiaImage? by mutableStateOf(null)
    private var nearFieldBitmap: ImageBitmap? by mutableStateOf(null)
    private var nearFieldOffsetX: Float by mutableStateOf(0f)
    private var nearFieldOffsetY: Float by mutableStateOf(0f)

    @Composable
    fun build(
        isRunning: Boolean,
        onFpsLabelChanged: (String) -> Unit = {}
    ) {
        val screenDimensions = remember { screenDimensionsService.getScreenDimensions() }
        Box(modifier = Modifier.size(width = screenDimensions.width.dp, height = screenDimensions.height.dp)) {
            LayerCanvas(bitmap = splashImageBitmap, screenDimensions = screenDimensions, isSplash = true)
            LayerCanvas(bitmap = farGroundBitmap, screenDimensions = screenDimensions, offsetX = farGroundOffsetX, offsetY = farGroundOffsetY)
            LayerCanvas(midGroundBitmap, screenDimensions = screenDimensions, offsetX = midGroundOffsetX, offsetY = midGroundOffsetY)
            ForegroundLayerCanvas(image = foregroundBitmap)
            LayerCanvas(nearFieldBitmap, screenDimensions = screenDimensions, offsetX = nearFieldOffsetX, offsetY = nearFieldOffsetY)
            LayerCanvas(collisionBitmap, screenDimensions = screenDimensions, offsetX = collisionOffsetX, offsetY = collisionOffsetY)
        }

        LaunchedEffect(isRunning) {
            onFpsLabelChanged(if (isRunning) "FPS: running" else "FPS: paused")
        }
    }

    @Composable
    private fun LayerCanvas(
        bitmap: ImageBitmap?,
        screenDimensions: ScreenDimensions,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        isSplash: Boolean = false
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .clip(RectangleShape)
        ) {
            bitmap?.let { image ->
                if (isSplash) {
                    drawImage(
                        image = image,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(image.width, image.height),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize((screenDimensions.width * density).toInt(), (screenDimensions.height * density).toInt())
                    )
                } else {
                    val scaledWidth = (image.width * density).toInt()
                    val scaledHeight = (image.height * density).toInt()
                    drawImage(
                        image = image,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(image.width, image.height),
                        dstOffset = IntOffset(
                            (-offsetX * density).toInt(),
                            (-offsetY * density).toInt()
                        ),
                        dstSize = IntSize(scaledWidth, scaledHeight)
                    )
                }
            }
        }
    }

    @Composable
    private fun ForegroundLayerCanvas(image: SkiaImage?) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .clip(RectangleShape)
        ) {
            image?.let { foreground ->
                drawIntoCanvas { canvas ->
                    canvas.skiaCanvas.drawImageRect(
                        image = foreground,
                        src = Rect.makeXYWH(0f, 0f, foreground.width.toFloat(), foreground.height.toFloat()),
                        dst = Rect.makeXYWH(0f, 0f, foreground.width.toFloat() * density, foreground.height.toFloat() * density),
                        samplingMode = SamplingMode.LINEAR,
                        paint = foregroundPaint,
                        strict = true
                    )
                }
            }
        }
    }

    fun drawSplash(image: ImageBitmap) {
        splashImageBitmap = image
    }

    fun drawFarGround(image: ImageBitmap, offsetX: Float = 0f, offsetY: Float = 0F) {
        farGroundBitmap = image
        farGroundOffsetX = offsetX
        farGroundOffsetY = offsetY
    }

    fun drawMidGround(image: ImageBitmap, offsetX: Float = 0F, offsetY: Float = 0F) {
        midGroundBitmap = image
        midGroundOffsetX = offsetX
        midGroundOffsetY = offsetY
    }

    fun drawForeground(image: SkiaImage) {
        foregroundBitmap?.close()
        foregroundBitmap = image
    }

    fun drawNearField(image: ImageBitmap, offsetX: Float = 0F, offsetY: Float = 0F) {
        nearFieldBitmap = image
        nearFieldOffsetX = offsetX
        nearFieldOffsetY = offsetY
    }

    fun drawCollision(image: ImageBitmap, offsetX: Float = 0F, offsetY: Float = 0F) {
        collisionBitmap = image
        collisionOffsetX = offsetX
        collisionOffsetY = offsetY
    }

    fun clearAllLayers() {
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
        foregroundBitmap?.close()
        foregroundBitmap = null
    }

}
