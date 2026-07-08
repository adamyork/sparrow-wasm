package com.github.adamyork.sparrow.wasm.engine

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.wasm.common.data.ControlAction
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.engine.data.Particle

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface Physics {

    fun applyPlayerPhysics(player: Player, collisionBoundaries: CollisionBoundaries, collision: Collision)

    fun applyPlayerCollisionPhysics(player: Player, rect: Rect?, viewPort: ViewPort)

    fun applyCollisionParticlePhysics(mapParticles: ArrayList<Particle>, viewPort: ViewPort)

    fun applyDustParticlePhysics(mapParticles: ArrayList<Particle>)

    fun applyProjectileParticlePhysics(mapParticles: ArrayList<Particle>, viewPort: ViewPort)

    fun applyMapItemReturnParticlePhysics(mapParticles: ArrayList<Particle>, viewPort: ViewPort)

    fun changeXVelocityIfDirectionChanged(controlAction: ControlAction, player: Player)

}
