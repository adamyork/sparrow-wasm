package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.adamyork.sparrow.wasm.common.data.ControlAction
import com.github.adamyork.sparrow.wasm.common.data.ControlType
import kotlinx.browser.window
import kotlinx.coroutines.awaitCancellation
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import kotlin.time.Duration.Companion.milliseconds

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class GameUiMain(
    private val controller: GameUiController,
    private val sparrowColorScheme: DefaultSparrowColorScheme
) {

    @Composable
    fun build() {
        val gameUiDrawLayer = remember { GameUiDrawLayer() }
        var fpsLabel by remember { mutableStateOf("FPS: --") }
        var gameStatusLabel by remember { mutableStateOf("Press Start To Begin") }
        var gameStatusLabelColor by remember { mutableStateOf(Color.Black) }
        var scoreLabel by remember { mutableStateOf("Score: --") }
        var totalLabel by remember { mutableStateOf("Total: --") }
        var remainingLabel by remember { mutableStateOf("Remaining: --") }
        var isRunning by remember { mutableStateOf(false) }
        var isLoadingChecklistVisible by remember { mutableStateOf(true) }
        var splashImage by remember { mutableStateOf<ImageBitmap?>(null) }

        val allTasksCompleted = controller.allTasksCompleted()

        val overlayBg = sparrowColorScheme.getScoreOverlayBackground()
        val disabledButtonColors = sparrowColorScheme.getDisabledButtonColors()
        val disabledBorder = sparrowColorScheme.getDisabledBorder()

        LaunchedEffect(allTasksCompleted, splashImage, isLoadingChecklistVisible) {
            if (allTasksCompleted && splashImage != null && isLoadingChecklistVisible) {
                kotlinx.coroutines.delay(2000.milliseconds)
                isLoadingChecklistVisible = false
                gameUiDrawLayer.drawSplash(splashImage!!)
            }
        }

        LaunchedEffect(Unit) {
            splashImage = controller.initializeGame()
            val scoreLabels = controller.getScoreLabels()
            scoreLabel = scoreLabels.scoreLabel
            totalLabel = scoreLabels.totalLabel
            remainingLabel = scoreLabels.remainingLabel
        }

        LaunchedEffect(Unit) {
            fun toControlAction(event: KeyboardEvent): ControlAction? {
                return when (event.key.lowercase()) {
                    "arrowleft" -> ControlAction.LEFT
                    "arrowright" -> ControlAction.RIGHT
                    " ", "space", "spacebar" -> ControlAction.JUMP
                    else -> null
                }
            }

            val keyDownListener: (Event) -> Unit = { event ->
                if (event is KeyboardEvent) {
                    val action = toControlAction(event)
                    if (action != null) {
                        event.preventDefault()
                        controller.applyInput(ControlType.START, action)
                    }
                }
            }

            val keyUpListener: (Event) -> Unit = { event ->
                if (event is KeyboardEvent) {
                    val action = toControlAction(event)
                    if (action != null) {
                        event.preventDefault()
                        controller.applyInput(ControlType.STOP, action)
                    }
                }
            }

            window.addEventListener("keydown", keyDownListener)
            window.addEventListener("keyup", keyUpListener)
            try {
                awaitCancellation()
            } finally {
                window.removeEventListener("keydown", keyDownListener)
                window.removeEventListener("keyup", keyUpListener)
            }
        }

        LaunchedEffect(isRunning) {
            if (isRunning) {
                var frameId: Int
                fun loop() {
                    val frame = controller.tick()
                    frame.drawResult.farGroundBitmap?.let { image ->
                        gameUiDrawLayer.drawFarGround(
                            image,
                            frame.drawResult.farGroundOffsetX,
                            frame.drawResult.farGroundOffsetY
                        )
                    }
                    frame.drawResult.midGroundBitmap?.let { image ->
                        gameUiDrawLayer.drawMidGround(
                            image,
                            frame.drawResult.midGroundOffsetX,
                            frame.drawResult.midGroundOffsetY
                        )
                    }
                    frame.drawResult.collisionBitmap?.let { image ->
                        gameUiDrawLayer.drawCollision(
                            image,
                            frame.drawResult.collisionOffsetX,
                            frame.drawResult.collisionOffsetY
                        )
                    }
                    frame.drawResult.foregroundSurface?.let { surface ->
                        gameUiDrawLayer.drawForeground(surface.makeImageSnapshot().toComposeImageBitmap())
                    }
                    fpsLabel = frame.fpsLabel
                    scoreLabel = frame.scoreLabel
                    totalLabel = frame.totalLabel
                    remainingLabel = frame.remainingLabel
                    gameStatusLabel = frame.gameStatusLabel
                    gameStatusLabelColor = frame.gameStatusLabelColor
                    frameId = window.requestAnimationFrame { _ ->
                        loop()
                    }
                }
                frameId = window.requestAnimationFrame { _ ->
                    loop()
                }
                try {
                    awaitCancellation()
                } finally {
                    window.cancelAnimationFrame(frameId)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "com.github.adamyork.sparrow.wasm.Main content area"
                }
                .testTag("compose-body-com.github.adamyork.sparrow.wasm.main")
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "com.github.adamyork.sparrow.wasm.Main content stack" }
                    .testTag("compose-body-com.github.adamyork.sparrow.wasm.main-column")
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .semantics { contentDescription = "canvas-with-fps-overlay" }
                        .testTag("canvas-with-fps-overlay")
                ) {
                    gameUiDrawLayer.build(
                        isRunning = isRunning,
                        onFpsLabelChanged = { nextLabel ->
                            fpsLabel = nextLabel
                        }
                    )
                    Text(
                        text = gameStatusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = gameStatusLabelColor,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .background(overlayBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .semantics { contentDescription = "centered-top-label" }
                            .testTag("centered-top-label")
                    )

                    if (isLoadingChecklistVisible) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(260.dp)
                                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            controller.loadingTasks.forEach { task ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Circle,
                                        contentDescription = null,
                                        tint = if (task.isCompleted) Color.Green else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = task.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (task.isCompleted) Color.White else Color.LightGray
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = fpsLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(overlayBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .semantics { contentDescription = "FPS label" }
                            .testTag("fps-label")
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(overlayBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .semantics { contentDescription = "score-overlay" }
                            .testTag("score-overlay"),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = scoreLabel, style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Text(text = totalLabel, style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Text(text = remainingLabel, style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .semantics { contentDescription = "start-pause-button-row" }
                        .testTag("start-pause-button-row")
                ) {

                    val focusManager = LocalFocusManager.current

                    Button(
                        onClick = {
                            isRunning = true
                            controller.start()
                            focusManager.clearFocus()
                        },
                        enabled = !isRunning,
                        modifier = Modifier
                            .semantics { contentDescription = "start-button" }
                            .testTag("start-button")
                    ) {
                        Text("Start")
                    }

                    Button(
                        onClick = {
                            isRunning = false
                            controller.pause()
                            focusManager.clearFocus()
                        },
                        enabled = isRunning,
                        colors = disabledButtonColors,
                        modifier = Modifier
                            .semantics { contentDescription = "pause-button" }
                            .testTag("pause-button")
                            .border(disabledBorder, RoundedCornerShape(6.dp))
                    ) {
                        Text("Pause")
                    }

                    Button(
                        onClick = {
                            controller.reset()
                            focusManager.clearFocus()
                        },
                        enabled = isRunning,
                        colors = disabledButtonColors,
                        modifier = Modifier
                            .semantics { contentDescription = "reset-button" }
                            .testTag("reset-button")
                            .border(disabledBorder, RoundedCornerShape(6.dp))
                    ) {
                        Text("reset")
                    }
                }
            }
        }
    }
}

