package com.github.adamyork.sparrow.platform.engine

import com.github.adamyork.sparrow.platform.common.data.enemy.Enemy
import com.github.adamyork.sparrow.platform.common.data.item.Item
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.engine.data.Particle
import com.github.adamyork.sparrow.platform.engine.data.ParticleWriteResult
import com.github.adamyork.sparrow.platform.service.AssetService

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface Particles {

    companion object {
        const val DEFAULT_GPU_PARTICLE_CAPACITY: Int = 4096
    }

    fun populateColorMap(assetService: AssetService)

    fun applyCollisionParticles(originX: Int, originY: Int, particles: ArrayList<Particle>)

    fun applyDustParticles(player: Player, particles: ArrayList<Particle>)

    fun applyProjectileParticle(player: Player, enemy: Enemy, particles: ArrayList<Particle>): Boolean

    fun applyMapItemReturnParticle(player: Player, mapItem: Item, particles: ArrayList<Particle>)

    fun createGpuParticleComputeBuffer(maxParticles: Int = DEFAULT_GPU_PARTICLE_CAPACITY): FloatArray {
        throw EngineException("GPU particle compute buffer is not implemented for this engine")
    }

    fun writeGpuParticleSpawnBuffer(
        player: Player,
        mapParticles: List<Particle>,
        targetBuffer: FloatArray,
        maxParticles: Int = DEFAULT_GPU_PARTICLE_CAPACITY,
        startSlot: Int = 0,
        previouslyWrittenSlots: List<Int> = emptyList()
    ): ParticleWriteResult {
        throw EngineException("GPU particle spawn buffer writes are not implemented for this engine")
    }

}
