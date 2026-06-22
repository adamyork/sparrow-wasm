package com.github.adamyork.sparrow.wasm.common

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.github.adamyork.sparrow.wasm.CustomImageWrapper
import org.jetbrains.skia.Image


class ImageHelper {

    companion object {

        fun toCustomImageWrapper(bytes: ByteArray): CustomImageWrapper {
            return try {
                val skiaImage = Image.makeFromEncoded(bytes)
                CustomImageWrapper(bytes, skiaImage.toComposeImageBitmap())
            } catch (e: Exception) {
                e.printStackTrace()
                CustomImageWrapper(bytes, ImageBitmap(0, 0))
            }
        }
    }
}