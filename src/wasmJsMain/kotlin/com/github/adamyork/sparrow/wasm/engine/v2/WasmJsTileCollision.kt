package com.github.adamyork.sparrow.wasm.engine.v2

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.v2.PlatformTileCollision
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
class WasmJsTileCollision(
    physics: Physics,
    scoreService: ScoreService
) : PlatformTileCollision(physics, scoreService) {

    override fun cacheCollisionPixels() {
        val image = Image.makeFromEncoded(collisionImage.bytes)
        val bitmap = Bitmap.makeFromImage(image)
        try {
            val pixelMap = bitmap.peekPixels() ?: throw IllegalStateException("Failed to peek pixels")
            populateTileMapFromPixelSource(bitmap.width, bitmap.height) { x, y ->
                pixelMap.getColor(x, y) == COLLISION_COLOR_VALUE
            }
        } finally {
            image.close()
        }
    }
}
