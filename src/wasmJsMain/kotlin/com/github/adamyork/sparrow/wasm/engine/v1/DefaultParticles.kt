package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.graphics.Color
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.data.Direction
import com.github.adamyork.sparrow.wasm.common.data.GameElement
import com.github.adamyork.sparrow.wasm.common.data.enemy.Enemy
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.engine.data.Particle
import com.github.adamyork.sparrow.wasm.engine.data.ParticleShape
import com.github.adamyork.sparrow.wasm.engine.data.ParticleType
import com.github.adamyork.sparrow.wasm.service.AssetService
import me.tatarka.inject.annotations.Inject
import kotlin.math.abs
import kotlin.random.Random

@AppScope
@Inject
/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class DefaultParticles : Particles {

    companion object {
        const val MAX_SQUARE_RADIAL_RADIUS: Int = 45
        const val MAX_ACTIVE_PROJECTILES: Int = 1
    }

    val dustParticleOffsets: HashMap<Int, Pair<Int, Int>> = HashMap()
    val colorMap: HashMap<ParticleType, Color> = HashMap()


    init {
        dustParticleOffsets[0] = Pair(10, 2)
        dustParticleOffsets[1] = Pair(6, 4)
        dustParticleOffsets[2] = Pair(14, 5)
        dustParticleOffsets[3] = Pair(20, 7)
        dustParticleOffsets[4] = Pair(27, 8)
        dustParticleOffsets[5] = Pair(34, 6)
        dustParticleOffsets[6] = Pair(40, 3)
        dustParticleOffsets[7] = Pair(32, 9)
        dustParticleOffsets[8] = Pair(25, 11)
        dustParticleOffsets[9] = Pair(18, 10)
        dustParticleOffsets[10] = Pair(11, 8)
        dustParticleOffsets[11] = Pair(22, 12)
    }


    override fun createCollisionParticles(originX: Int, originY: Int): ArrayList<Particle> {
        val intRange = 0..360
        return intRange.toList().toIntArray().map {
            Particle(
                it,
                originX,
                originY,
                originX,
                originY,
                2,
                2,
                ParticleType.COLLISION,
                0,
                20,
                Random.nextInt(50),
                Random.nextInt(50),
                1,
                colorMap[ParticleType.COLLISION] ?: Color.White,
                ParticleShape.RECT
            )
        }.toCollection(ArrayList())
    }

    override fun createDustParticles(player: Player): ArrayList<Particle> {
        val footY = player.y + player.height - (player.height / 10)
        val intRange = 0..11
        return intRange.toList().toIntArray().map {
            val offsetX = dustParticleOffsets[it]?.first ?: 0
            val offsetY = dustParticleOffsets[it]?.second ?: 0
            val diameter = ((it * 3) + 6).coerceAtMost(30)
            val anchorX = if (player.direction == Direction.LEFT) {
                player.x + player.width - (player.width / 4) + offsetX
            } else {
                player.x + (player.width / 4) - offsetX
            }
            val particleX = anchorX - (diameter / 2)
            val particleY = (footY - offsetY) - (diameter / 2)
            val color = colorMap[ParticleType.DUST] ?: Color.White
            val adjustedAlpha = (color.alpha * (1f - (it / 11f))).coerceIn(0f, 1f)
            val adjustedAlphaColor = Color(color.red, color.green, color.blue, adjustedAlpha)
            Particle(
                it,
                particleX,
                particleY,
                player.x,
                player.y,
                diameter,
                diameter,
                ParticleType.DUST,
                0,
                5,
                0,
                0,
                0,
                adjustedAlphaColor,
                ParticleShape.CIRCLE
            )
        }.toCollection(ArrayList())
    }

    override fun createProjectileParticle(
        player: Player,
        enemy: Enemy,
        particles: ArrayList<Particle>
    ): Pair<ArrayList<Particle>, Boolean> {
        val count = particles.filter { it.type == ParticleType.PROJECTILE }
            .size
        val particles: ArrayList<Particle> = ArrayList()
        var particleAdded = false
        if (count < MAX_ACTIVE_PROJECTILES) {
            val xDiff = abs((enemy as GameElement).x - player.x)
            val yDiff = abs(enemy.y - player.y)
            val xIncrement = (xDiff / 10).coerceAtLeast(1)
            val yIncrement = (yDiff / 10).coerceAtLeast(1)
            particleAdded = true
            particles.add(
                Particle(
                    count + 1,
                    enemy.x,
                    enemy.y,
                    player.x,
                    player.y,
                    24,
                    24,
                    ParticleType.PROJECTILE,
                    0,
                    10,
                    xIncrement,
                    yIncrement,
                    1,
                    colorMap[ParticleType.PROJECTILE] ?: Color.White,
                    ParticleShape.CIRCLE
                )
            )
        }
        return Pair(particles, particleAdded)
    }

    override fun createMapItemReturnParticle(player: Player): Particle {
        return Particle(
            0,
            player.x,
            player.y,
            player.x,
            player.y,
            32,
            32,
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
        val playerMovementAlpha = assetService.gameConfig.particle.player.movement.color.a.toFloat()
        val playerMovementRed = assetService.gameConfig.particle.player.movement.color.r.toFloat()
        val playerMovementGreen = assetService.gameConfig.particle.player.movement.color.g.toFloat()
        val playerMovementBlue = assetService.gameConfig.particle.player.movement.color.b.toFloat()

        val playerCollisionAlpha = assetService.gameConfig.particle.player.collision.color.a.toFloat()
        val playerCollisionRed = assetService.gameConfig.particle.player.collision.color.r.toFloat()
        val playerCollisionGreen = assetService.gameConfig.particle.player.collision.color.g.toFloat()
        val playerCollisionBlue = assetService.gameConfig.particle.player.collision.color.b.toFloat()

        val enemyProjectileAlpha = assetService.gameConfig.particle.enemy.projectile.color.a.toFloat()
        val enemyProjectileRed = assetService.gameConfig.particle.enemy.projectile.color.r.toFloat()
        val enemyProjectileGreen = assetService.gameConfig.particle.enemy.projectile.color.g.toFloat()
        val enemyProjectileBlue = assetService.gameConfig.particle.enemy.projectile.color.b.toFloat()

        val playerMovementParticleColor = Color(
            playerCollisionRed,
            playerCollisionGreen,
            playerCollisionBlue,
            playerCollisionAlpha
        )
        val playerCollisionParticleColor = Color(
            playerMovementRed,
            playerMovementGreen,
            playerMovementBlue,
            playerMovementAlpha
        )
        val enemyProjectileParticleColor = Color(
            enemyProjectileRed,
            enemyProjectileGreen,
            enemyProjectileBlue,
            enemyProjectileAlpha
        )
        colorMap[ParticleType.DUST] = playerMovementParticleColor
        colorMap[ParticleType.COLLISION] = playerCollisionParticleColor
        colorMap[ParticleType.PROJECTILE] = enemyProjectileParticleColor
    }

}
