package com.github.adamyork.sparrow.android.engine.v1

import android.graphics.BitmapFactory
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.v1.PlatformCollision
import com.github.adamyork.sparrow.platform.service.ScoreService
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class AndroidCollision(
    physics: Physics,
    scoreService: ScoreService
) : PlatformCollision(physics, scoreService) {

    override fun cacheCollisionPixels() {
        val bitmap = BitmapFactory.decodeByteArray(collisionImage.bytes, 0, collisionImage.bytes.size)
            ?: error("Failed to decode byte array into Android Bitmap")
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        collisionMask = BooleanArray(bitmapWidth * bitmapHeight)
        val pixels = IntArray(bitmapWidth * bitmapHeight)
        bitmap.getPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight)
        for (y in 0 until bitmapHeight) {
            val rowOffset = y * bitmapWidth
            for (x in 0 until bitmapWidth) {
                val pixelColor = pixels[rowOffset + x]
                collisionMask[rowOffset + x] = (pixelColor == COLLISION_COLOR_VALUE)
            }
        }
    }

}
