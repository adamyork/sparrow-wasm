package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

class ComposeScreenLayer {

    private var imageBitmap: ImageBitmap? by mutableStateOf(null)

    @Composable
    fun build(
        isRunning: Boolean,
        onFpsLabelChanged: (String) -> Unit = {}
    ) {

        Canvas(
            modifier = Modifier
                .size(width = 1024.dp, height = 768.dp)
                .background(Color.White)
                .focusable()

        ) {
            imageBitmap?.let { bitmap ->
                drawImage(
                    image = bitmap,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
            }
        }

        LaunchedEffect(isRunning) {
            onFpsLabelChanged(if (isRunning) "FPS: running" else "FPS: paused")
        }
    }

    fun draw(image: ImageBitmap) {
        imageBitmap = runCatching {
            image
        }.getOrNull()
    }
}
