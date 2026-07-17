package com.github.adamyork.sparrow.wasm.common

import org.khronos.webgl.Int8Array
import org.w3c.files.Blob

external interface VisualViewport {
    val height: Double
    val width: Double
}

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalWasmJsInterop::class)
fun createBlobFromInt8Array(int8Array: Int8Array): Blob = js("new Blob([int8Array])")

@OptIn(ExperimentalWasmJsInterop::class)
fun getVisualViewport(): VisualViewport = js(
    """(window.visualViewport || { 
        height: window.innerHeight, 
        width: window.innerWidth 
    })"""
)