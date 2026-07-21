package com.github.adamyork.sparrow.platform.gui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.github.adamyork.sparrow.platform.engine.data.PlatformImage
import com.github.adamyork.sparrow.platform.gui.data.ScreenDimensions

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
abstract class PlatformUiDrawLayer(
    protected val screenDimensionsService: ScreenDimensionsService
) {

    abstract var foregroundPaint: Any
    abstract var foregroundBitmap: Any?

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
    private var nearFieldBitmap: ImageBitmap? by mutableStateOf(null)
    private var nearFieldOffsetX: Float by mutableStateOf(0f)
    private var nearFieldOffsetY: Float by mutableStateOf(0f)

    @Composable
    fun Build(
        isRunning: Boolean,
        onFpsLabelChanged: (String) -> Unit = {}
    ) {
        val screenDimensions = remember { screenDimensionsService.getScreenDimensions() }
        Box(modifier = Modifier.size(width = screenDimensions.width.dp, height = screenDimensions.height.dp)) {
            LayerCanvas(bitmap = splashImageBitmap, screenDimensions = screenDimensions, isSplash = true)
            LayerCanvas(
                bitmap = farGroundBitmap,
                screenDimensions = screenDimensions,
                offsetX = farGroundOffsetX,
                offsetY = farGroundOffsetY
            )
            LayerCanvas(
                midGroundBitmap,
                screenDimensions = screenDimensions,
                offsetX = midGroundOffsetX,
                offsetY = midGroundOffsetY
            )
            ForegroundLayerCanvas(image = foregroundBitmap)
            LayerCanvas(
                nearFieldBitmap,
                screenDimensions = screenDimensions,
                offsetX = nearFieldOffsetX,
                offsetY = nearFieldOffsetY
            )
            LayerCanvas(
                collisionBitmap,
                screenDimensions = screenDimensions,
                offsetX = collisionOffsetX,
                offsetY = collisionOffsetY
            )
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
                        dstSize = IntSize(
                            (screenDimensions.width * density).toInt(),
                            (screenDimensions.height * density).toInt()
                        )
                    )
                } else {
                    val viewportWidth = screenDimensions.width.coerceAtMost(image.width)
                    val viewportHeight = screenDimensions.height.coerceAtMost(image.height)
                    val maxSrcX = (image.width - viewportWidth).coerceAtLeast(0)
                    val maxSrcY = (image.height - viewportHeight).coerceAtLeast(0)
                    val srcX = offsetX.toInt().coerceIn(0, maxSrcX)
                    val srcY = offsetY.toInt().coerceIn(0, maxSrcY)
                    val dstWidth = (screenDimensions.width * density).toInt()
                    val dstHeight = (screenDimensions.height * density).toInt()
                    drawImage(
                        image = image,
                        srcOffset = IntOffset(srcX, srcY),
                        srcSize = IntSize(viewportWidth, viewportHeight),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(dstWidth, dstHeight)
                    )
                }
            }
        }
    }

    @Composable
    protected open fun ForegroundLayerCanvas(image: Any?) {
        throw RuntimeException("must implement")
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

    open fun drawForeground(image: PlatformImage) {
        throw RuntimeException("must implement")
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

    open fun clearAllLayers() {
        throw RuntimeException("must implement")
    }

}
