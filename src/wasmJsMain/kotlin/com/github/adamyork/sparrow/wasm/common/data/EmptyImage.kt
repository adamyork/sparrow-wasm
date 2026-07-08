package com.github.adamyork.sparrow.wasm.common.data

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class EmptyImage private constructor() {
    companion object {
        fun createEmptyImage(): Image {
            val bitmap = Bitmap().apply {
                allocN32Pixels(1, 1)
                erase(0x00000000) // Transparent
            }
            return Image.makeFromBitmap(bitmap)
        }
    }
}