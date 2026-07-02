package com.github.adamyork.sparrow.wasm.engine

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.engine.data.Particle

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface Physics {

    fun applyPlayerPhysics(
        player: Player,
        collisionBoundaries: CollisionBoundaries,
        collision: Collision
    ): Player

    fun applyPlayerCollisionPhysics(player: Player, rect: Rect?, viewPort: ViewPort): Player

    fun applyCollisionParticlePhysics(mapParticles: ArrayList<Particle>, viewPort: ViewPort): ArrayList<Particle>

    fun applyDustParticlePhysics(mapParticles: ArrayList<Particle>): ArrayList<Particle>

    fun applyProjectileParticlePhysics(mapParticles: ArrayList<Particle>, viewPort: ViewPort): ArrayList<Particle>

    fun applyMapItemReturnParticlePhysics(mapParticles: ArrayList<Particle>): ArrayList<Particle>

}
