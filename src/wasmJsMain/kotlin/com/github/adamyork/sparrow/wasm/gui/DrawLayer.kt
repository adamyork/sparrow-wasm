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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

class DrawLayer {

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
    private var foregroundBitmap: ImageBitmap? by mutableStateOf(null)
    private var foregroundOffsetX: Float by mutableStateOf(0f)
    private var foregroundOffsetY: Float by mutableStateOf(0f)

    @Composable
    fun build(
        isRunning: Boolean,
        onFpsLabelChanged: (String) -> Unit = {}
    ) {
        Box(modifier = Modifier.size(width = 1024.dp, height = 768.dp)) {
            LayerCanvas(bitmap = splashImageBitmap)
            LayerCanvas(bitmap = farGroundBitmap, offsetX = farGroundOffsetX, offsetY = farGroundOffsetY)
            LayerCanvas(midGroundBitmap, offsetX = midGroundOffsetX, offsetY = midGroundOffsetY)
            LayerCanvas(foregroundBitmap, offsetX = foregroundOffsetX, offsetY = foregroundOffsetY)
            LayerCanvas(collisionBitmap, offsetX = collisionOffsetX, offsetY = collisionOffsetY)
        }

        LaunchedEffect(isRunning) {
            onFpsLabelChanged(if (isRunning) "FPS: running" else "FPS: paused")
        }
    }

    @Composable
    private fun LayerCanvas(bitmap: ImageBitmap?, offsetX: Float = 0f, offsetY: Float = 0f) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .clip(RectangleShape)
        ) {
            bitmap?.let { image ->
                val scaledWidth = (image.width * density).toInt()
                val scaledHeight = (image.height * density).toInt()
                drawImage(
                    image = image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(image.width, image.height),
                    dstOffset = IntOffset(
                        (-offsetX * density).toInt(),
                        (-offsetY * density).toInt()
                    ), // Scale offsets too!
                    dstSize = IntSize(scaledWidth, scaledHeight)
                )
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

    fun drawForeground(image: ImageBitmap, offsetX: Float = 0F, offsetY: Float = 0F) {
        foregroundBitmap = image
        foregroundOffsetX = offsetX
        foregroundOffsetY = offsetY
    }

    fun drawCollision(image: ImageBitmap, offsetX: Float = 0F, offsetY: Float = 0F) {
        collisionBitmap = image
        collisionOffsetX = offsetX
        collisionOffsetY = offsetY
    }
}
