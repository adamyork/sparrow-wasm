package com.github.adamyork.sparrow.wasm.common

import org.jetbrains.skia.Surface

interface StatusProvider {

    var running: Boolean
    var lastPaintTime: Long
    var lastBackgroundComposite: Surface?

    fun getDeltaTime(): Double

    fun getFps(): Double
}