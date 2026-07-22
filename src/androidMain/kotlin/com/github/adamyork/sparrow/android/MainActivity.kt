package com.github.adamyork.sparrow.android

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.lifecycleScope
import com.github.adamyork.sparrow.android.gui.AndroidPortraitGui
import com.github.adamyork.sparrow.platform.LogConfig
import com.github.adamyork.sparrow.platform.gui.PlatformGame
import com.github.adamyork.sparrow.platform.gui.SparrowColorScheme
import com.github.adamyork.sparrow.platform.gui.UiScaffold
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogConfig.initialize(minimumLevel = Level.INFO)
        logger.info { "app started" }
        val component = AppConfig::class.create()
        val gameLayer = component.game
        val sparrowColorScheme = component.sparrowColorScheme
        setContent {
            ScaffoldDelegate(component, gameLayer, sparrowColorScheme)
        }
        lifecycleScope.launch(Dispatchers.Main) {
            component.platformInterop.onReady {
                component.platformInterop.hidePlatformLoader()
            }
        }
    }

    @Composable
    private fun ScaffoldDelegate(
        component: AppConfig,
        gameLayer: PlatformGame,
        sparrowColorScheme: SparrowColorScheme
    ) {
        val configuration = LocalConfiguration.current
        component.screenDimensionsService.initialize(
            configuration.screenWidthDp,
            configuration.screenHeightDp
        )
        logger.info { "screen dimensions updated: ${component.screenDimensionsService.getScreenDimensions()}" }
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        if (isPortrait) {
            AndroidPortraitGui().BuildGui()
        } else {
            UiScaffold().BuildGui(gameLayer, sparrowColorScheme)
        }
    }

}
