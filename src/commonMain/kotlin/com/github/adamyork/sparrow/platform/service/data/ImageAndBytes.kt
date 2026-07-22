package com.github.adamyork.sparrow.platform.service.data

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class ImageAndBytes(val bytes: ByteArray, val imageBitmap: ImageBitmap) {

    override fun equals(other: Any?): Boolean {
        return this === other || other is ImageAndBytes &&
                bytes.contentEquals(other.bytes) &&
                imageBitmap == other.imageBitmap
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + imageBitmap.hashCode()
        return result
    }

}