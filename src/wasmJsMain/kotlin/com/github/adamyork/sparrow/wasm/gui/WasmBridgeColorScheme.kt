package com.github.adamyork.sparrow.wasm.gui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.tatarka.inject.annotations.Inject

@Inject
class WasmBridgeColorScheme : ComposeColorScheme {

    // --- Light Mode Palette Definitions ---
    private val lightBgColor = Color(0xFFF8FAFC)        // --bg-color
    private val lightCardBg = Color(0xFFFFFFFF)         // --card-bg
    private val lightTextMain = Color(0xFF0F172A)       // --text-com.github.adamyork.sparrow.wasm.main
    private val lightTextMuted = Color(0xFF64748B)      // --text-muted
    private val lightPrimary = Color(0xFF4F46E5)        // --primary
    private val lightPrimaryHover = Color(0xFF6366F1)   // --primary-hover
    private val lightBorderColor = Color(0xFFE2E8F0)    // --border-color

    // --- Dark Mode Palette Definitions ---
    private val darkBgColor = Color(0xFF0F172A)         // --bg-color
    private val darkCardBg = Color(0xFF1E293B)          // --card-bg
    private val darkTextMain = Color(0xFFF8FAFC)        // --text-com.github.adamyork.sparrow.wasm.main
    private val darkTextMuted = Color(0xFF94A3B8)       // --text-muted
    private val darkPrimary = Color(0xFF818CF8)         // --primary
    private val darkPrimaryHover = Color(0xFFA5B4FC)    // --primary-hover
    private val darkBorderColor = Color(0xFF334155)     // --border-color

    // --- Core Design Token Mappings ---
    private val primaryColor
        @Composable
        get() = if (isSystemInDarkTheme()) darkPrimary else lightPrimary
    private val primaryHoverColor
        @Composable
        get() = if (isSystemInDarkTheme()) darkPrimaryHover else lightPrimaryHover
    private val textMain
        @Composable
        get() = if (isSystemInDarkTheme()) darkTextMain else lightTextMain
    private val textMuted
        @Composable
        get() = if (isSystemInDarkTheme()) darkTextMuted else lightTextMuted
    private val backgroundColor
        @Composable
        get() = if (isSystemInDarkTheme()) darkBgColor else lightBgColor
    private val cardBackground
        @Composable
        get() = if (isSystemInDarkTheme()) darkCardBg else lightCardBg
    private val borderColor
        @Composable
        get() = if (isSystemInDarkTheme()) darkBorderColor else lightBorderColor

    // --- Custom Token Overrides ---
    // CSS fallback: color-mix(in srgb, var(--primary) 30%, transparent)
    private val focusOutlineColor
        @Composable
        get() = primaryColor.copy(alpha = 0.30f)

    // CSS fallback: opacity: 0.55;
    private val disabledOpacity = 0.55f

    @Composable
    override fun getScheme(): ColorScheme {
        return if (isSystemInDarkTheme()) {
            darkColorScheme(
                primary = primaryColor,
                background = backgroundColor,
                surface = backgroundColor,               // Matches html, body mapping
                surfaceContainer = cardBackground,       // .content-block, header, footer mapping
                onBackground = textMain,                 // var(--text-com.github.adamyork.sparrow.wasm.main)
                onSurface = textMain,                    // var(--text-com.github.adamyork.sparrow.wasm.main)
                onSurfaceVariant = textMuted,            // var(--text-muted) for subtext
                outline = borderColor,                   // Standard borders
                outlineVariant = borderColor             // Dividers and structural borders
            )
        } else {
            lightColorScheme(
                primary = primaryColor,
                background = backgroundColor,
                surface = backgroundColor,
                surfaceContainer = cardBackground,
                onBackground = textMain,
                onSurface = textMain,
                onSurfaceVariant = textMuted,
                outline = borderColor,
                outlineVariant = borderColor
            )
        }
    }

    @Composable
    override fun getHoverColor(): Color = primaryHoverColor

    @Composable
    override fun getFocusOutlineColor(): Color = focusOutlineColor

    @Composable
    override fun getDisabledOpacity(): Float = disabledOpacity
}
