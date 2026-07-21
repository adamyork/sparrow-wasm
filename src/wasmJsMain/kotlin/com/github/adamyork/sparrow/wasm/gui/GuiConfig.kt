package com.github.adamyork.sparrow.wasm.gui

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.gui.*
import me.tatarka.inject.annotations.Provides

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface GuiConfig {

    val game: PlatformGame
    val sparrowColorScheme: SparrowColorScheme
    val screenDimensionsService: ScreenDimensionsService

    @AppScope
    @Provides
    fun provideGameLayer(impl: WasmJsGame): PlatformGame = impl


    @AppScope
    @Provides
    fun provideSparrowColorScheme(impl: DefaultSparrowColorScheme): SparrowColorScheme = impl

    @AppScope
    @Provides
    fun provideScreenDimensionsService(impl: DefaultScreenDimensionsService): ScreenDimensionsService = impl

}
