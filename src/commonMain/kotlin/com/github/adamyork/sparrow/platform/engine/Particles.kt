package com.github.adamyork.sparrow.platform.engine

import com.github.adamyork.sparrow.platform.common.data.enemy.Enemy
import com.github.adamyork.sparrow.platform.common.data.item.Item
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.engine.data.Particle
import com.github.adamyork.sparrow.platform.service.AssetService

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface Particles {

    fun populateColorMap(assetService: AssetService)

    fun applyCollisionParticles(originX: Int, originY: Int, particles: ArrayList<Particle>)

    fun applyDustParticles(player: Player, particles: ArrayList<Particle>)

    fun applyProjectileParticle(player: Player, enemy: Enemy, particles: ArrayList<Particle>): Boolean

    fun applyMapItemReturnParticle(player: Player, mapItem: Item, particles: ArrayList<Particle>)

}
