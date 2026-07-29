package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.CommonColorCollision
import com.github.adamyork.sparrow.platform.service.ScoreService
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class WasmJsColorCollision(
    physics: Physics,
    scoreService: ScoreService
) : CommonColorCollision(physics, scoreService) {

    override fun cacheCollisionPixels() {
        val image = Image.makeFromEncoded(collisionImage.bytes)
        val bitmap = Bitmap.makeFromImage(image)
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        // TODO custom error
        val pixelMap = bitmap.peekPixels() ?: throw IllegalStateException("Failed to peek pixels")
        collisionMask = BooleanArray(bitmapWidth * bitmapHeight)
        for (y in 0 until bitmapHeight) {
            val rowOffset = y * bitmapWidth
            for (x in 0 until bitmapWidth) {
                collisionMask[rowOffset + x] = (pixelMap.getColor(x, y) == COLLISION_COLOR_VALUE)
            }
        }
    }

}
