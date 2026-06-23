package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

class ComposeScreenLayer {

    private var backgroundBitmap: ImageBitmap? by mutableStateOf(null)
    private var foregroundBitmap: ImageBitmap? by mutableStateOf(null)

    @Composable
    fun build(
        isRunning: Boolean,
        onFpsLabelChanged: (String) -> Unit = {}
    ) {
        // We use Box to stack the layers.
        // The background is rendered first, the foreground on top.
        Box(modifier = Modifier.size(width = 1024.dp, height = 768.dp)) {

            // Background Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                backgroundBitmap?.let { bitmap ->
                    drawImage(image = bitmap, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                }
            }

            // Foreground Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                foregroundBitmap?.let { bitmap ->
                    drawImage(image = bitmap, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                }
            }
        }

        LaunchedEffect(isRunning) {
            onFpsLabelChanged(if (isRunning) "FPS: running" else "FPS: paused")
        }
    }

    // Dedicated function for background updates
    fun drawBackground(image: ImageBitmap) {
        backgroundBitmap = image
    }

    // Dedicated function for foreground updates
    fun drawForeground(image: ImageBitmap) {
        foregroundBitmap = image
    }
}
