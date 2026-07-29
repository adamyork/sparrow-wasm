package com.github.adamyork.sparrow.android.engine

import android.graphics.BitmapFactory
import androidx.core.graphics.get
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.engine.EngineException
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.CommonTileCollision
import com.github.adamyork.sparrow.platform.service.ScoreService
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class AndroidTileCollision(
    physics: Physics,
    scoreService: ScoreService
) : CommonTileCollision(physics, scoreService) {

    override fun cacheCollisionPixels() {
        val bytes = collisionImage.bytes
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw EngineException("Failed to decode collision image bytes")
        try {
            val width = bitmap.width
            val height = bitmap.height
            populateTileMapFromPixelSource(width, height) { x, y ->
                bitmap[x, y] == COLLISION_COLOR_VALUE
            }
        } finally {
            bitmap.recycle()
        }
    }

}
