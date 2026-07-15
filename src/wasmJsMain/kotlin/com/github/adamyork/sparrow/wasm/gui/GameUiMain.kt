package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.common.data.ControlAction
import com.github.adamyork.sparrow.wasm.common.data.ControlType
import com.github.adamyork.sparrow.wasm.common.data.GameLifeCycleState
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState
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
    private val statusProvider: StatusProvider,
    private val screenDimensionsService: ScreenDimensionsService
) {

    @Composable
    fun build() {
        val screenDimensions = remember { screenDimensionsService.getScreenDimensions() }
        val gameUiDrawLayer = remember { GameUiDrawLayer(screenDimensionsService) }
        var fpsLabel by remember { mutableStateOf("FPS: --") }
        var gameStatusLabel by remember { mutableStateOf("Press Start To Begin") }
        var scoreLabel by remember { mutableStateOf("Score: --") }
        var totalLabel by remember { mutableStateOf("Total: --") }
        var remainingLabel by remember { mutableStateOf("Remaining: --") }
        var isLoadingChecklistVisible by remember { mutableStateOf(true) }
        val isTouchDevice = remember { window.navigator.maxTouchPoints > 0 }
        val allTasksCompleted = controller.allTasksCompleted()
        val gameLifeCycleState = statusProvider.gameLifeCycleState
        val splashImage = controller.gameStateElements.splashImage
        val endingImage = controller.gameStateElements.endingImage
        val colorScheme = MaterialTheme.colorScheme
        val overlayBg = colorScheme.inverseSurface
        val disabledButtonColors = androidx.compose.material3.ButtonDefaults.buttonColors(
            disabledContainerColor = colorScheme.secondaryContainer,
            disabledContentColor = colorScheme.onSecondaryContainer
        )
        val textMainColor = colorScheme.onSurface

        LaunchedEffect(allTasksCompleted, splashImage, isLoadingChecklistVisible) {
            if (allTasksCompleted && isLoadingChecklistVisible) {
                kotlinx.coroutines.delay(1000.milliseconds)
                isLoadingChecklistVisible = false
                gameUiDrawLayer.drawSplash(splashImage)
            }
        }

        LaunchedEffect(Unit) {
            controller.initializeGame()
            val stateElements = controller.gameStateElements
            scoreLabel = stateElements.scoreLabel
            totalLabel = stateElements.totalLabel
            remainingLabel = stateElements.remainingLabel
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
                if (statusProvider.gameLifeCycleState == GameLifeCycleState.RUNNING && event is KeyboardEvent) {
                    val action = toControlAction(event)
                    if (action != null) {
                        event.preventDefault()
                        controller.applyInput(ControlType.START, action)
                    }
                }
            }

            val keyUpListener: (Event) -> Unit = { event ->
                if (statusProvider.gameLifeCycleState == GameLifeCycleState.RUNNING && event is KeyboardEvent) {
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

        LaunchedEffect(statusProvider.gameLifeCycleState == GameLifeCycleState.RUNNING) {
            if (statusProvider.gameLifeCycleState == GameLifeCycleState.RUNNING) {
                var frameId: Int
                fun loop(timestamp: Double) {
                    statusProvider.setCurrentFrameTime(timestamp)
                    val frame = controller.tick(timestamp)
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
                    frame.drawResult.nearFieldBitmap?.let { image ->
                        gameUiDrawLayer.drawNearField(
                            image,
                            frame.drawResult.nearFieldOffsetX,
                            frame.drawResult.nearFieldOffsetY
                        )
                    }
                    frame.drawResult.collisionBitmap?.let { image ->
                        gameUiDrawLayer.drawCollision(
                            image,
                            frame.drawResult.collisionOffsetX,
                            frame.drawResult.collisionOffsetY
                        )
                    }
                    frame.drawResult.foregroundImage?.let { image ->
                        gameUiDrawLayer.drawForeground(image)
                    }
                    fpsLabel = frame.fpsLabel
                    scoreLabel = frame.scoreLabel
                    totalLabel = frame.totalLabel
                    remainingLabel = frame.remainingLabel
                    gameStatusLabel = frame.gameStatusLabel
                    if (frame.completionTransitionRequested) {
                        gameUiDrawLayer.clearAllLayers()
                        endingImage.let { gameUiDrawLayer.drawSplash(it) }
                        statusProvider.gameMapState = GameMapState.COMPLETED
                        statusProvider.gameLifeCycleState = GameLifeCycleState.COMPLETED
                        return
                    }
                    frameId = window.requestAnimationFrame { timestamp -> loop(timestamp) }
                }
                frameId = window.requestAnimationFrame { timestamp -> loop(timestamp) }
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
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .semantics { contentDescription = "Main content area" }
                .testTag("main")
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            if (statusProvider.gameLifeCycleState == GameLifeCycleState.RUNNING && isTouchDevice) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            controller.applyInput(ControlType.START, ControlAction.LEFT)
                            tryAwaitRelease()
                            controller.applyInput(ControlType.STOP, ControlAction.LEFT)
                        })
                    })
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            controller.applyInput(ControlType.START, ControlAction.JUMP)
                        })
                    })
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            controller.applyInput(ControlType.START, ControlAction.RIGHT)
                            tryAwaitRelease()
                            controller.applyInput(ControlType.STOP, ControlAction.RIGHT)
                        })
                    })
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Main content stack" }
                    .testTag("main-column")
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentSize()
                        .semantics { contentDescription = "canvas-with-fps-overlay" }
                        .testTag("canvas-with-fps-overlay")
                ) {
                    gameUiDrawLayer.build(
                        isRunning = statusProvider.gameLifeCycleState == GameLifeCycleState.RUNNING,
                        onFpsLabelChanged = { nextLabel ->
                            fpsLabel = nextLabel
                        }
                    )
                    if (gameLifeCycleState != GameLifeCycleState.COMPLETED) {
                        Text(
                            text = gameStatusLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = textMainColor,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .background(overlayBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .semantics { contentDescription = "centered-top-label" }
                                .testTag("centered-top-label")
                        )
                    }

                    if (isLoadingChecklistVisible) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(260.dp)
                                .background(overlayBg, RoundedCornerShape(8.dp))
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
                                        color = textMainColor
                                    )
                                }
                            }
                        }
                    }

                    if (gameLifeCycleState != GameLifeCycleState.INITIALIZING && gameLifeCycleState != GameLifeCycleState.COMPLETED) {
                        Text(
                            text = fpsLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = textMainColor,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(overlayBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .semantics { contentDescription = "FPS label" }
                                .testTag("fps-label")
                        )
                    }

                    if (gameLifeCycleState != GameLifeCycleState.INITIALIZING && gameLifeCycleState != GameLifeCycleState.COMPLETED) {
                        Text(
                            text = "Screen: ${screenDimensions.width}x${screenDimensions.height}",
                            style = MaterialTheme.typography.labelLarge,
                            color = textMainColor,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .padding(top = 36.dp)
                                .background(overlayBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .semantics { contentDescription = "Screen dimensions label" }
                                .testTag("screen-dimensions-label")
                        )
                    }

                    if (gameLifeCycleState != GameLifeCycleState.INITIALIZING && gameLifeCycleState != GameLifeCycleState.COMPLETED) {
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
                            Text(text = scoreLabel, style = MaterialTheme.typography.labelLarge, color = textMainColor)
                            Text(text = totalLabel, style = MaterialTheme.typography.labelLarge, color = textMainColor)
                            Text(
                                text = remainingLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = textMainColor
                            )
                        }
                    }
                }
                if (gameLifeCycleState != GameLifeCycleState.INITIALIZING) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .semantics { contentDescription = "start-pause-button-row" }
                            .testTag("start-pause-button-row")
                            .align(Alignment.CenterHorizontally)
                    ) {

                        val focusManager = LocalFocusManager.current

                        Button(
                            onClick = {
                                controller.start()
                                focusManager.clearFocus()
                            },
                            enabled = gameLifeCycleState != GameLifeCycleState.RUNNING || gameLifeCycleState == GameLifeCycleState.COMPLETED,
                            colors = disabledButtonColors,
                            modifier = Modifier
                                .semantics { contentDescription = "start-button" }
                                .testTag("start-button")
                        ) {
                            Text("Start", color = textMainColor)
                        }

                        Button(
                            onClick = {
                                controller.pause()
                                focusManager.clearFocus()
                            },
                            enabled = gameLifeCycleState == GameLifeCycleState.RUNNING,
                            colors = disabledButtonColors,
                            modifier = Modifier
                                .semantics { contentDescription = "pause-button" }
                                .testTag("pause-button")
                        ) {
                            Text("Pause", color = textMainColor)
                        }

                        Button(
                            onClick = {
                                controller.reset()
                                focusManager.clearFocus()
                            },
                            enabled = gameLifeCycleState == GameLifeCycleState.RUNNING,
                            colors = disabledButtonColors,
                            modifier = Modifier
                                .semantics { contentDescription = "reset-button" }
                                .testTag("reset-button")
                        ) {
                            Text("Reset", color = textMainColor)
                        }
                    }
                }
            }
        }
    }
}
