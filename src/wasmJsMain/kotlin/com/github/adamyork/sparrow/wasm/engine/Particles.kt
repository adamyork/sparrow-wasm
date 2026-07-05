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

    fun createCollisionParticles(originX: Int, originY: Int): ArrayList<Particle>

    fun createDustParticles(player: Player, particles: ArrayList<Particle>)

    fun createProjectileParticle(
        player: Player,
        enemy: Enemy,
        particles: ArrayList<Particle>
    ): Boolean

    fun createMapItemReturnParticle(player: Player, particles: ArrayList<Particle>)

}
