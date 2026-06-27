package com.github.adamyork.sparrow.wasm.gui

import com.github.adamyork.sparrow.wasm.AppScope
import me.tatarka.inject.annotations.Provides

interface GuiConfig {

    val gameLayer: GameLayer
    val sparrowColorScheme: SparrowColorScheme

    @AppScope
    @Provides
    fun provideGameLayer(impl: DefaultGameLayer): GameLayer = impl


    @AppScope
    @Provides
    fun provideSparrowColorScheme(impl: DefaultSparrowColorScheme): SparrowColorScheme = impl

}
