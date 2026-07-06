package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.wasm.common.data.enemy.Enemy
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.data.Particle
import com.github.adamyork.sparrow.wasm.service.AssetService

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface Particles {

    fun populateColorMap(assetService: AssetService)

    fun applyCollisionParticles(originX: Int, originY: Int, particles: ArrayList<Particle>)

    fun applyDustParticles(player: Player, particles: ArrayList<Particle>)

    fun applyProjectileParticle(
        player: Player,
        enemy: Enemy,
        particles: ArrayList<Particle>
    ): Boolean

    fun applyMapItemReturnParticle(player: Player, particles: ArrayList<Particle>)

}
