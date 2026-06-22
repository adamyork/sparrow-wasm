package com.github.adamyork.sparrow.wasm.gui

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.ComposeMain
import me.tatarka.inject.annotations.Provides

interface GuiConfig {

    @ComposeMain
    val composeBodyMain: BodyElement

    val wasmBridgeColorScheme: WasmBridgeColorScheme

    @AppScope
    @Provides
    @ComposeMain
    fun provideComposeBodyMain(impl: GameScreen): BodyElement = impl


    @AppScope
    @Provides
    fun provideWasmBridgeColorScheme(impl: WasmBridgeColorScheme): ComposeColorScheme = impl

}
