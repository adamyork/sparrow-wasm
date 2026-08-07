package com.github.adamyork.sparrow.android.gui

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.gui.*
import me.tatarka.inject.annotations.Provides

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface GuiConfig {

    val game: Game
    val platformQuitUi: QuitUi
    val sparrowColorScheme: SparrowColorScheme
    val screenDimensionsService: ScreenDimensionsService

    @AppScope
    @Provides
    fun provideGameLayer(impl: AndroidGame): Game = impl

    @AppScope
    @Provides
    fun providePlatformQuitUi(impl: AndroidQuitUi): QuitUi = impl


    @AppScope
    @Provides
    fun provideSparrowColorScheme(impl: CommonSparrowColorScheme): SparrowColorScheme = impl

    @AppScope
    @Provides
    fun provideScreenDimensionsService(impl: CommonScreenDimensionsService): ScreenDimensionsService = impl

}
