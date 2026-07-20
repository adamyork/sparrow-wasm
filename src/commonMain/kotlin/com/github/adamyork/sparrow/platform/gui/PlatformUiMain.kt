package com.github.adamyork.sparrow.platform.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
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
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.ControlAction
import com.github.adamyork.sparrow.platform.common.data.ControlType
import com.github.adamyork.sparrow.platform.common.data.LifeCycleState
import com.github.adamyork.sparrow.platform.common.data.map.GameMapState
import com.github.adamyork.sparrow.platform.service.RuntimeService
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
abstract class PlatformUiMain(
    val controller: UiController,
    val runtimeService: RuntimeService,
    val screenDimensionsService: ScreenDimensionsService,
    val platformInterop: PlatformInterop
) {

    abstract var uiDrawLayer: PlatformUiDrawLayer

    @Composable
    fun Build() {
        val screenDimensions = remember { screenDimensionsService.getScreenDimensions() }
        val uiDrawLayer = remember { uiDrawLayer }
        var fpsLabel by remember { mutableStateOf("FPS: --") }
        var gameStatusLabel by remember { mutableStateOf("Press Start To Begin") }
        var scoreLabel by remember { mutableStateOf("Score: --") }
        var totalLabel by remember { mutableStateOf("Total: --") }
        var remainingLabel by remember { mutableStateOf("Remaining: --") }
        var isLoadingChecklistVisible by remember { mutableStateOf(true) }
        val isTouchDevice = remember { platformInterop.isTouchDevice() }
        val allTasksCompleted = controller.allTasksCompleted()
        val gameLifeCycleState = runtimeService.lifeCycleState
        val splashImage = controller.stateElements.splashImage
        val endingImage = controller.stateElements.endingImage
        val colorScheme = MaterialTheme.colorScheme
        val overlayBg = colorScheme.inverseSurface
        val disabledButtonColors = ButtonDefaults.buttonColors(
            disabledContainerColor = colorScheme.secondaryContainer,
            disabledContentColor = colorScheme.onSecondaryContainer
        )
        val textMainColor = colorScheme.onSurface

        LaunchedEffect(allTasksCompleted, splashImage, isLoadingChecklistVisible) {
            if (allTasksCompleted && isLoadingChecklistVisible) {
                delay(1000.milliseconds)
                isLoadingChecklistVisible = false
                uiDrawLayer.drawSplash(splashImage)
            }
        }

        LaunchedEffect(Unit) {
            controller.initializeGame()
            val stateElements = controller.stateElements
            scoreLabel = stateElements.scoreLabel
            totalLabel = stateElements.totalLabel
            remainingLabel = stateElements.remainingLabel
        }

        platformInterop.InsertInputHandlers(controller, runtimeService)

        LaunchedEffect(runtimeService.lifeCycleState == LifeCycleState.RUNNING) {
            if (runtimeService.lifeCycleState == LifeCycleState.RUNNING) {
                var frameId: Int
                fun loop(timestamp: Double) {
                    runtimeService.setCurrentFrameTime(timestamp)
                    val frame = controller.tick(timestamp)
                    frame.drawResult.farGroundBitmap?.let { image ->
                        uiDrawLayer.drawFarGround(
                            image,
                            frame.drawResult.farGroundOffsetX,
                            frame.drawResult.farGroundOffsetY
                        )
                    }
                    frame.drawResult.midGroundBitmap?.let { image ->
                        uiDrawLayer.drawMidGround(
                            image,
                            frame.drawResult.midGroundOffsetX,
                            frame.drawResult.midGroundOffsetY
                        )
                    }
                    frame.drawResult.nearFieldBitmap?.let { image ->
                        uiDrawLayer.drawNearField(
                            image,
                            frame.drawResult.nearFieldOffsetX,
                            frame.drawResult.nearFieldOffsetY
                        )
                    }
                    frame.drawResult.collisionBitmap?.let { image ->
                        uiDrawLayer.drawCollision(
                            image,
                            frame.drawResult.collisionOffsetX,
                            frame.drawResult.collisionOffsetY
                        )
                    }
                    frame.drawResult.foregroundImage?.let { image ->
                        uiDrawLayer.drawForeground(image)
                    }
                    fpsLabel = frame.fpsLabel
                    scoreLabel = frame.scoreLabel
                    totalLabel = frame.totalLabel
                    remainingLabel = frame.remainingLabel
                    gameStatusLabel = frame.gameStatusLabel
                    if (frame.completionTransitionRequested) {
                        uiDrawLayer.clearAllLayers()
                        endingImage.let { uiDrawLayer.drawSplash(it) }
                        runtimeService.gameMapState = GameMapState.COMPLETED
                        runtimeService.lifeCycleState = LifeCycleState.COMPLETED
                        return
                    }
                    frameId = platformInterop.requestAnimationFrame { timestamp -> loop(timestamp) }
                }
                frameId = platformInterop.requestAnimationFrame { timestamp -> loop(timestamp) }
                try {
                    awaitCancellation()
                } finally {
                    platformInterop.cancelAnimationFrame(frameId)
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
            if (runtimeService.lifeCycleState == LifeCycleState.RUNNING && isTouchDevice) {
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
                    uiDrawLayer.Build(
                        isRunning = runtimeService.lifeCycleState == LifeCycleState.RUNNING,
                        onFpsLabelChanged = { nextLabel ->
                            fpsLabel = nextLabel
                        }
                    )
                    if (gameLifeCycleState != LifeCycleState.COMPLETED) {
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

                    if (gameLifeCycleState != LifeCycleState.INITIALIZING && gameLifeCycleState != LifeCycleState.COMPLETED) {
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

                    if (gameLifeCycleState != LifeCycleState.INITIALIZING && gameLifeCycleState != LifeCycleState.COMPLETED) {
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

                    if (gameLifeCycleState != LifeCycleState.INITIALIZING && gameLifeCycleState != LifeCycleState.COMPLETED) {
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
                if (gameLifeCycleState != LifeCycleState.INITIALIZING) {
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
                            enabled = gameLifeCycleState != LifeCycleState.RUNNING || gameLifeCycleState == LifeCycleState.COMPLETED,
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
                            enabled = gameLifeCycleState == LifeCycleState.RUNNING,
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
                            enabled = gameLifeCycleState == LifeCycleState.RUNNING,
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
