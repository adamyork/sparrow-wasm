package com.github.adamyork.sparrow.android

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.github.adamyork.sparrow.platform.LogConfig
import com.github.adamyork.sparrow.platform.gui.UiScaffold
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        LogConfig.initialize(minimumLevel = Level.INFO)
        logger.info { "app started" }
        val component = AppConfig::class.create()
        val configuration = resources.configuration
        component.screenDimensionsService.initialize(configuration.screenWidthDp, configuration.screenHeightDp)
        logger.info { "screen dimensions: ${component.screenDimensionsService.getScreenDimensions()}" }
        val gameLayer = component.game
        val sparrowColorScheme = component.sparrowColorScheme
        setContent {
            UiScaffold().BuildGui(gameLayer, sparrowColorScheme)
        }
        lifecycleScope.launch(Dispatchers.Main) {
            component.platformInterop.onReady {
                component.platformInterop.hidePlatformLoader()
            }
        }
    }

}
