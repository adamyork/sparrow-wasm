package com.github.adamyork.sparrow.platform.engine.data

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.github.adamyork.sparrow.platform.common.data.ViewPort

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class Particle(
    val id: Int,
    var x: Int,
    var y: Int,
    val originX: Int,
    val originY: Int,
    var width: Int,
    var height: Int,
    val type: ParticleType,
    var frame: Int,
    val lifetime: Int,
    val xJitter: Int,
    val yJitter: Int,
    var radius: Int,
    val color: Color,
    val shape: ParticleShape
) {
    companion object {
        private const val VISIBILITY_BUFFER = 50
        fun hasActiveVisibleCollisionParticles(mapParticles: Collection<Particle>, viewPort: ViewPort): Boolean {
            return mapParticles.any { particle -> particle.isActiveVisibleCollisionParticle(viewPort) }
        }
    }

    fun toRect() = Rect(
        x.toFloat(),
        y.toFloat(),
        (x + width).toFloat(),
        (y + height).toFloat()
    )

    fun cullingCheck(viewPort: ViewPort): Boolean {
        val localCord = viewPort.globalToLocal(x, y)
        return localCord.second < viewPort.height &&
            localCord.second > -VISIBILITY_BUFFER &&
            localCord.first > -VISIBILITY_BUFFER &&
            localCord.first < viewPort.width + VISIBILITY_BUFFER
    }

    fun isActiveVisibleCollisionParticle(viewPort: ViewPort): Boolean {
        if (type != ParticleType.COLLISION) {
            return false
        }
        val isAlive = frame < lifetime
        val isVisible = cullingCheck(viewPort)
        return isAlive && isVisible
    }
}
