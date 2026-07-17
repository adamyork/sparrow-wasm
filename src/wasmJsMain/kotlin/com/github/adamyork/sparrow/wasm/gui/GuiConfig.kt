package com.github.adamyork.sparrow.wasm.gui

import com.github.adamyork.sparrow.platform.AppScope
import me.tatarka.inject.annotations.Provides

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface GuiConfig {

    val game: Game
    val sparrowColorScheme: SparrowColorScheme
    val screenDimensionsService: ScreenDimensionsService

    @AppScope
    @Provides
    fun provideGameLayer(impl: DefaultGame): Game = impl


    @AppScope
    @Provides
    fun provideSparrowColorScheme(impl: DefaultSparrowColorScheme): SparrowColorScheme = impl

    @AppScope
    @Provides
    fun provideScreenDimensionsService(impl: DefaultScreenDimensionsService): ScreenDimensionsService = impl

}
