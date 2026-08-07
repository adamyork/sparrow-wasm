package com.github.adamyork.sparrow.android

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.lifecycleScope
import com.github.adamyork.sparrow.android.common.AndroidInterop
import com.github.adamyork.sparrow.android.gui.AndroidPortraitGui
import com.github.adamyork.sparrow.platform.LogConfig
import com.github.adamyork.sparrow.platform.gui.Game
import com.github.adamyork.sparrow.platform.gui.SparrowColorScheme
import com.github.adamyork.sparrow.platform.gui.UiScaffold
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogConfig.initialize(minimumLevel = Level.INFO)
        logger.info { "MainActivity onCreate invoked" }
        val component = AppConfig::class.create()
        (component.platformInterop as? AndroidInterop)?.initialize(this)
        val gameLayer = component.game
        val sparrowColorScheme = component.sparrowColorScheme
        setContent {
            ScaffoldDelegate(component, gameLayer, sparrowColorScheme)
        }
        lifecycleScope.launch {
            component.platformInterop.onReady {
                component.platformInterop.hidePlatformLoader()
            }
        }
    }

    @SuppressLint("ConfigurationScreenWidthHeight")
    @Composable
    private fun ScaffoldDelegate(
        component: AppConfig,
        gameLayer: Game,
        sparrowColorScheme: SparrowColorScheme
    ) {
        val configuration = LocalConfiguration.current
        component.screenDimensionsService.initialize(
            configuration.screenWidthDp,
            configuration.screenHeightDp
        )
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        if (isPortrait) {
            AndroidPortraitGui().BuildGui()
        } else {
            UiScaffold().BuildGui(gameLayer, sparrowColorScheme)
        }
    }

}
