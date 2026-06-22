package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.wasm.data.enemy.Enemy
import com.github.adamyork.sparrow.wasm.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.data.Particle
import com.github.adamyork.sparrow.wasm.service.AssetService

interface Particles {

    fun populateColorMap(assetService: AssetService)

    fun createCollisionParticles(originX: Int, originY: Int): ArrayList<Particle>

    fun createDustParticles(player: Player): ArrayList<Particle>

    fun createProjectileParticle(
        player: Player,
        enemy: Enemy,
        particles: ArrayList<Particle>
    ): Pair<ArrayList<Particle>, Boolean>

    fun createMapItemReturnParticle(player: Player): Particle

}
