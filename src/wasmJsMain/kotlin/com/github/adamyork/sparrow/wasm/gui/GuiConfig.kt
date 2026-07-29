package com.github.adamyork.sparrow.wasm.gui

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
    fun provideGameLayer(impl: WasmJsGame): Game = impl

    @AppScope
    @Provides
    fun providePlatformQuitUi(impl: WasmQuitUi): QuitUi = impl


    @AppScope
    @Provides
    fun provideSparrowColorScheme(impl: CommonSparrowColorScheme): SparrowColorScheme = impl

    @AppScope
    @Provides
    fun provideScreenDimensionsService(impl: CommonScreenDimensionsService): ScreenDimensionsService = impl

}
