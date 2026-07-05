package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.graphics.Color
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.data.Direction
import com.github.adamyork.sparrow.wasm.common.data.enemy.Enemy
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.engine.data.Particle
import com.github.adamyork.sparrow.wasm.engine.data.ParticleShape
import com.github.adamyork.sparrow.wasm.engine.data.ParticleType
import com.github.adamyork.sparrow.wasm.service.AssetService
import me.tatarka.inject.annotations.Inject
import kotlin.random.Random

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultParticles : Particles {

    companion object {
        const val MAX_SQUARE_RADIAL_RADIUS: Int = 45
        private const val MAX_ACTIVE_PROJECTILES: Int = 1
        private const val COLLISION_PARTICLE_COUNT: Int = 360
        private const val PROJECTILE_SIZE: Int = 24
        private const val MAP_ITEM_RETURN_SIZE: Int = 32
        private const val BASE_DIAMETER_MULTIPLIER = 3
        private const val BASE_DIAMETER_MULTIPLIER_BUFFER = 6
        private const val DUST_Y_OFFSET = 10
        private const val DIAMETER_MAX = 30
    }

    private val dustParticleOffsets = listOf(
        10 to 2, 6 to 4, 14 to 5, 20 to 7, 27 to 8, 34 to 6,
        40 to 3, 32 to 9, 25 to 11, 18 to 10, 11 to 8, 22 to 12
    )

    private var colorMap: Map<ParticleType, Color> = emptyMap()
    private val collisionPool = ArrayList<Particle>(COLLISION_PARTICLE_COUNT)
    private val dustParticleList = ArrayList<Particle>(dustParticleOffsets.size)

    override fun createCollisionParticles(originX: Int, originY: Int): ArrayList<Particle> {
        val collisionColor = colorMap[ParticleType.COLLISION] ?: Color.White
        if (collisionPool.isEmpty()) {
            repeat(COLLISION_PARTICLE_COUNT) { index ->
                collisionPool.add(
                    Particle(
                        index,
                        originX,
                        originY,
                        originX,
                        originY,
                        4,
                        4,
                        ParticleType.COLLISION,
                        0,
                        20,
                        Random.nextInt(50),
                        Random.nextInt(50),
                        1,
                        collisionColor,
                        ParticleShape.RECT
                    )
                )
            }
        }
        collisionPool.forEachIndexed { i, p ->
            collisionPool[i] = p.copy(
                x = originX,
                y = originY,
                originX = originX,
                originY = originY,
                frame = 0
            )
        }
        return collisionPool
    }

    override fun createDustParticles(player: Player): ArrayList<Particle> {
        dustParticleList.clear()
        val footY = player.y + player.height - (player.height / DUST_Y_OFFSET)
        val color = colorMap[ParticleType.DUST] ?: Color.White
        dustParticleOffsets.forEachIndexed { index, (offsetX, offsetY) ->
            val diameter = ((index * BASE_DIAMETER_MULTIPLIER) + BASE_DIAMETER_MULTIPLIER_BUFFER)
                .coerceAtMost(DIAMETER_MAX)
            val anchorX = if (player.direction == Direction.LEFT) {
                player.x + player.width - (player.width / 4) + offsetX
            } else {
                player.x + (player.width / 4) - offsetX
            }
            val particleX = anchorX - (diameter / 2)
            val particleY = (footY - offsetY) - (diameter / 2)
            dustParticleList.add(
                Particle(
                    index,
                    particleX,
                    particleY,
                    player.x,
                    player.y,
                    diameter,
                    diameter,
                    ParticleType.DUST,
                    0,
                    20,
                    0,
                    0,
                    0,
                    color,
                    ParticleShape.CIRCLE
                )
            )
        }
        return dustParticleList
    }

    override fun createProjectileParticle(
        player: Player,
        enemy: Enemy,
        particles: ArrayList<Particle>
    ): Pair<ArrayList<Particle>, Boolean> {
        val count = getActiveProjectileCount(particles)
        if (count >= MAX_ACTIVE_PROJECTILES) return particles to false
        particles.add(
            Particle(
                count + 1,
                enemy.x,
                enemy.y,
                player.x,
                player.y,
                PROJECTILE_SIZE,
                PROJECTILE_SIZE,
                ParticleType.PROJECTILE,
                0,
                50,
                enemy.x,
                enemy.y,
                1,
                colorMap[ParticleType.PROJECTILE] ?: Color.White,
                ParticleShape.CIRCLE
            )
        )
        return particles to true
    }

    override fun createMapItemReturnParticle(player: Player): Particle {
        return Particle(
            0,
            player.x,
            player.y,
            player.x,
            player.y,
            MAP_ITEM_RETURN_SIZE,
            MAP_ITEM_RETURN_SIZE,
            ParticleType.MAP_ITEM_RETURN,
            0,
            16,
            0,
            0,
            1,
            Color.White,
            ParticleShape.RECT
        )
    }

    override fun populateColorMap(assetService: AssetService) {
        colorMap = mapOf(
            ParticleType.DUST to Color(
                assetService.gameConfig.particle.player.movement.color.r.toFloat(),
                assetService.gameConfig.particle.player.movement.color.g.toFloat(),
                assetService.gameConfig.particle.player.movement.color.b.toFloat(),
                assetService.gameConfig.particle.player.movement.color.a.toFloat()
            ),
            ParticleType.COLLISION to Color(
                assetService.gameConfig.particle.player.collision.color.r.toFloat(),
                assetService.gameConfig.particle.player.collision.color.g.toFloat(),
                assetService.gameConfig.particle.player.collision.color.b.toFloat(),
                assetService.gameConfig.particle.player.collision.color.a.toFloat()
            ),
            ParticleType.PROJECTILE to Color(
                assetService.gameConfig.particle.enemy.projectile.color.r.toFloat(),
                assetService.gameConfig.particle.enemy.projectile.color.g.toFloat(),
                assetService.gameConfig.particle.enemy.projectile.color.b.toFloat(),
                assetService.gameConfig.particle.enemy.projectile.color.a.toFloat()
            )
        )
    }

    private fun getActiveProjectileCount(particles: ArrayList<Particle>): Int =
        particles.count { it.type == ParticleType.PROJECTILE }

}
