package com.github.adamyork.sparrow.wasm

import androidx.compose.ui.graphics.ImageBitmap


data class CustomImageWrapper(val bytes: ByteArray, val imageBitmap: ImageBitmap) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CustomImageWrapper

        if (!bytes.contentEquals(other.bytes)) return false
        if (imageBitmap != other.imageBitmap) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + imageBitmap.hashCode()
        return result
    }
}
