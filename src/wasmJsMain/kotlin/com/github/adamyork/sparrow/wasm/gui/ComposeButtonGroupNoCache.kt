package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adamyork.sparrow.wasm.service.RandomNumberService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ComposeButtonGroupNoCache {

    @Composable
    fun build(
        uiScope: CoroutineScope,
        randomNumberService: RandomNumberService,
        composeColorScheme: ComposeColorScheme
    ) {
        var labelText by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        // Add an enabled state to demonstrate how disabledOpacity works
        val isEnabled = true

        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        val isPressed by interactionSource.collectIsPressedAsState()
        val isFocused by interactionSource.collectIsFocusedAsState() // 1. Track focus state

        val baseColor = composeColorScheme.getScheme().primary

        val animatedBackgroundColor by animateColorAsState(
            targetValue = if (isHovered) composeColorScheme.getHoverColor() else baseColor,
            animationSpec = tween(durationMillis = 200),
            label = "ButtonHoverAnimation"
        )

        Row(
            modifier = Modifier
                .widthIn(max = 450.dp)
                .fillMaxWidth()
                .semantics { contentDescription = "Random number section" }
                .testTag("button-group-no-cache")
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Button(
                onClick = {
                    uiScope.launch {
                        try {
                            val randomNum = randomNumberService.getRandomNumber()
                            labelText = randomNum.toString()
                            isError = false
                        } catch (t: Throwable) {
                            labelText = "Error loading number"
                            isError = true
                        }
                    }
                },
                enabled = isEnabled, // Pass the enabled state here
                interactionSource = interactionSource,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = animatedBackgroundColor,
                    contentColor = Color.White,
                    // 2. Use your custom opacity token for the disabled background state
                    disabledContainerColor = animatedBackgroundColor.copy(alpha = composeColorScheme.getDisabledOpacity()),
                    disabledContentColor = Color.White.copy(alpha = composeColorScheme.getDisabledOpacity())
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                modifier = Modifier
                    // 3. Dynamically apply your focus outline color when focused
                    .border(
                        width = if (isFocused) 2.dp else 0.dp,
                        color = if (isFocused) composeColorScheme.getFocusOutlineColor() else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .graphicsLayer {
                        translationY = if (isPressed && isEnabled) 1.dp.toPx() else 0f
                    }
                    .semantics { contentDescription = "Generate random number" }
                    .testTag("button-generate-no-cache")
            ) {
                Text(
                    text = "Generate Random Number",
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 1.sp
                )
            }

            if (labelText.isNotEmpty()) {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .semantics { contentDescription = if (isError) "Random number error" else "Random number result" }
                        .testTag("no-cache-result-text")
                )
            }
        }
    }
}
