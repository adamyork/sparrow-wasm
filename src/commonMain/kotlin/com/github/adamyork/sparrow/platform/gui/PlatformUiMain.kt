package com.github.adamyork.sparrow.platform.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.ControlAction
import com.github.adamyork.sparrow.platform.common.data.ControlType
import com.github.adamyork.sparrow.platform.common.data.LifeCycleState
import com.github.adamyork.sparrow.platform.common.data.map.GameMapState
import com.github.adamyork.sparrow.platform.service.RuntimeService
import com.github.adamyork.sparrow.platform.service.data.LoadingTaskStatus
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
    val platformInterop: PlatformInterop,
    val platformQuitUi: PlatformQuitUi
) {

    abstract var uiDrawLayer: PlatformUiDrawLayer
    protected open val centerHudWithinViewport: Boolean = false
    protected open val hudTopInset: Dp = 72.dp
    protected open val hudOverlayTopPadding: Dp = 8.dp

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
        val splashImage = controller.splashImage
        val endingImage = controller.endingImage
        val focusManager = LocalFocusManager.current
        val colorScheme = MaterialTheme.colorScheme
        val overlayBg = Color(0xB3000000)
        val showLabels = !isLoadingChecklistVisible && (
            gameLifeCycleState == LifeCycleState.INITIALIZED ||
                gameLifeCycleState == LifeCycleState.RUNNING ||
                gameLifeCycleState == LifeCycleState.PAUSED
            )
        val showButtons = gameLifeCycleState != LifeCycleState.INITIALIZING
        val disabledButtonColors = ButtonDefaults.buttonColors(
            disabledContainerColor = colorScheme.secondaryContainer,
            disabledContentColor = colorScheme.onSecondaryContainer
        )
        val textMainColor = colorScheme.onSurface
        val overlayTextColor = Color.White
        val hudTopOffset = hudTopInset
        val hudScale = remember(screenDimensions) {
            minOf(
                1f,
                minOf(
                    screenDimensions.width / 1024f,
                    screenDimensions.height / 768f
                ).coerceAtLeast(0.6f)
            )
        }
        val density = LocalDensity.current
        var viewportTopInRootPx by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(allTasksCompleted, splashImage, isLoadingChecklistVisible) {
            if (allTasksCompleted && splashImage != null && isLoadingChecklistVisible) {
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
                        endingImage?.let { uiDrawLayer.drawSplash(it) }
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

        LaunchedEffect(gameLifeCycleState) {
            if (gameLifeCycleState == LifeCycleState.RUNNING) {
                // Force-clear any focused control so mobile button chrome is not retained in gameplay.
                focusManager.clearFocus(force = true)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Main content area" }
                .testTag("main")
                .background(color = MaterialTheme.colorScheme.surfaceContainer),
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
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentSize()
                        .onGloballyPositioned { coordinates ->
                            viewportTopInRootPx = coordinates.positionInRoot().y
                        }
                        .semantics { contentDescription = "canvas-with-fps-overlay" }
                        .testTag("canvas-with-fps-overlay")
                ) {
                    uiDrawLayer.Build(
                        isRunning = runtimeService.lifeCycleState == LifeCycleState.RUNNING,
                        onFpsLabelChanged = { nextLabel ->
                            fpsLabel = nextLabel
                        }
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = hudOverlayTopPadding)
                    .zIndex(5f)
            ) {
                val measuredViewportTopOffset = if (centerHudWithinViewport) {
                    with(density) { viewportTopInRootPx.toDp() }
                } else {
                    0.dp
                }
                val hudContainerModifier = if (centerHudWithinViewport) {
                    Modifier
                        .align(Alignment.TopCenter)
                        .width(screenDimensions.width.dp)
                        .height(screenDimensions.height.dp)
                        .padding(top = measuredViewportTopOffset)
                } else {
                    Modifier.fillMaxSize()
                }

                Box(modifier = hudContainerModifier) {
                    if (showLabels) {
                        Text(
                            text = gameStatusLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = overlayTextColor,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = hudTopOffset)
                                .background(overlayBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .semantics { contentDescription = "centered-top-label" }
                                .testTag("centered-top-label")
                        )
                    }

                    if (showLabels) {
                        Text(
                            text = fpsLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = overlayTextColor,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = hudTopOffset, end = 12.dp)
                                .background(overlayBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .semantics { contentDescription = "FPS label" }
                                .testTag("fps-label")
                        )

                        Text(
                            text = "Screen: ${screenDimensions.width}x${screenDimensions.height}",
                            style = MaterialTheme.typography.labelLarge,
                            color = overlayTextColor,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = hudTopOffset + 24.dp, end = 12.dp)
                                .background(overlayBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .semantics { contentDescription = "Screen dimensions label" }
                                .testTag("screen-dimensions-label")
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = hudTopOffset, start = 12.dp)
                                .background(overlayBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .semantics { contentDescription = "score-overlay" }
                                .testTag("score-overlay"),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(text = scoreLabel, style = MaterialTheme.typography.labelLarge, color = overlayTextColor)
                            Text(text = totalLabel, style = MaterialTheme.typography.labelLarge, color = overlayTextColor)
                            Text(
                                text = remainingLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = overlayTextColor
                            )
                        }
                    }
                }
            }

            if (isLoadingChecklistVisible) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                        .zIndex(6f)
                ) {
                    val availableChecklistHeight = (maxHeight - 24.dp).coerceAtLeast(1.dp)
                    val estimatedChecklistHeight = (controller.loadingTasks.size * 30).dp + 48.dp
                    val checklistScale = (availableChecklistHeight / estimatedChecklistHeight).coerceIn(0.62f, 1f)

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(260.dp)
                            .graphicsLayer(
                                scaleX = checklistScale,
                                scaleY = checklistScale,
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            )
                            .background(overlayBg, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        controller.loadingTasks.forEach { task ->
                            val statusIcon = when (task.status) {
                                LoadingTaskStatus.COMPLETED -> Icons.Default.CheckCircle
                                LoadingTaskStatus.FAILED -> Icons.Default.Cancel
                                LoadingTaskStatus.PENDING -> Icons.Default.Circle
                            }
                            val statusColor = when (task.status) {
                                LoadingTaskStatus.COMPLETED -> Color.Green
                                LoadingTaskStatus.FAILED -> Color.Red
                                LoadingTaskStatus.PENDING -> Color.Gray
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = task.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = overlayTextColor
                                )
                            }
                        }
                    }
                }
            }

            if (showButtons) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .graphicsLayer(
                            scaleX = hudScale,
                            scaleY = hudScale,
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        )
                        .semantics { contentDescription = "start-pause-button-row" }
                        .testTag("start-pause-button-row")
                ) {

                    Button(
                        onClick = {
                            controller.start()
                            focusManager.clearFocus(force = true)
                        },
                        enabled = gameLifeCycleState != LifeCycleState.RUNNING || gameLifeCycleState == LifeCycleState.COMPLETED,
                        colors = disabledButtonColors,
                        modifier = Modifier
                            .focusProperties { canFocus = false }
                            .semantics { contentDescription = "start-button" }
                            .testTag("start-button")
                    ) {
                        Text("Start", color = textMainColor)
                    }

                    Button(
                        onClick = {
                            controller.pause()
                            focusManager.clearFocus(force = true)
                        },
                        enabled = gameLifeCycleState == LifeCycleState.RUNNING,
                        colors = disabledButtonColors,
                        modifier = Modifier
                            .focusProperties { canFocus = false }
                            .semantics { contentDescription = "pause-button" }
                            .testTag("pause-button")
                    ) {
                        Text("Pause", color = textMainColor)
                    }

                    Button(
                        onClick = {
                            controller.reset()
                            focusManager.clearFocus(force = true)
                        },
                        enabled = gameLifeCycleState == LifeCycleState.RUNNING,
                        colors = disabledButtonColors,
                        modifier = Modifier
                            .focusProperties { canFocus = false }
                            .semantics { contentDescription = "reset-button" }
                            .testTag("reset-button")
                    ) {
                        Text("Reset", color = textMainColor)
                    }

                    platformQuitUi.BuildQuitButton(
                        focusManager = focusManager,
                        disabledButtonColors = disabledButtonColors,
                        textMainColor = textMainColor
                    )
                }
            }
        }
    }
}
