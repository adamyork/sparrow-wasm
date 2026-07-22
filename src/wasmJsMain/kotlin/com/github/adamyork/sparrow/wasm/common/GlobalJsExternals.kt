package com.github.adamyork.sparrow.wasm.common

import org.khronos.webgl.Int8Array
import org.w3c.files.Blob

external interface VisualViewport {
    val height: Double
    val width: Double
}

@OptIn(ExperimentalWasmJsInterop::class)
fun createBlobFromInt8Array(@Suppress("UNUSED_PARAMETER") int8Array: Int8Array): Blob =
    js("new Blob([int8Array])")

@OptIn(ExperimentalWasmJsInterop::class)
fun getVisualViewport(): VisualViewport = js(
    """(window.visualViewport || { 
        height: window.innerHeight, 
        width: window.innerWidth 
    })"""
)