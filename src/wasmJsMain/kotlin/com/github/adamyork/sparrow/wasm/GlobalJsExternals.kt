package com.github.adamyork.sparrow.wasm

external interface VisualViewport {
    val height: Double
}

@OptIn(ExperimentalWasmJsInterop::class)
fun getVisualViewport(): VisualViewport = js("(window.visualViewport || { height: window.innerHeight })")