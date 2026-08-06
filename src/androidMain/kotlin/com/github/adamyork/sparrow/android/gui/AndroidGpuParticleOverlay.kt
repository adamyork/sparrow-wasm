package com.github.adamyork.sparrow.android.gui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.adamyork.sparrow.android.engine.AndroidGpuParticleRuntime

@Composable
fun AndroidGpuParticleOverlay() {
    val runtime by AndroidGpuParticleRuntime.observeActiveRuntime().collectAsState()
    val activeRuntime = runtime ?: return
    if (!activeRuntime.isEnabled()) return
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            activeRuntime.createSurfaceView(context)
        }
    )
}


