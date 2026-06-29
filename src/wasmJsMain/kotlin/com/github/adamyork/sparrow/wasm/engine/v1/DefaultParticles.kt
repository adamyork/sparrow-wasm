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

    private val dustParticleOffsets = listOf(
        10 to 2, 6 to 4, 14 to 5, 20 to 7, 27 to 8, 34 to 6,
        40 to 3, 32 to 9, 25 to 11, 18 to 10, 11 to 8, 22 to 12
    )

    val colorMap: HashMap<ParticleType, Color> = HashMap()

    override fun createCollisionParticles(originX: Int, originY: Int): ArrayList<Particle> {
        return (0..360).map {
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
        }.toCollection(ArrayList(360))
    }

    override fun createDustParticles(player: Player): ArrayList<Particle> {
        val footY = player.y + player.height - (player.height / 10)
        return (0..11).map {
            val (offsetX, offsetY) = dustParticleOffsets[it]
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
        }.toCollection(ArrayList(12))
    }

    override fun createProjectileParticle(
        player: Player,
        enemy: Enemy,
        particles: ArrayList<Particle>
    ): Pair<ArrayList<Particle>, Boolean> {
        val count = particles.filter { it.type == ParticleType.PROJECTILE }
            .size
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
        colorMap[ParticleType.DUST] = buildColor(
            assetService.gameConfig.particle.player.movement.color.a.toFloat(),
            assetService.gameConfig.particle.player.movement.color.r.toFloat(),
            assetService.gameConfig.particle.player.movement.color.g.toFloat(),
            assetService.gameConfig.particle.player.movement.color.b.toFloat()
        )
        colorMap[ParticleType.COLLISION] = buildColor(
            assetService.gameConfig.particle.player.collision.color.a.toFloat(),
            assetService.gameConfig.particle.player.collision.color.r.toFloat(),
            assetService.gameConfig.particle.player.collision.color.g.toFloat(),
            assetService.gameConfig.particle.player.collision.color.b.toFloat()
        )
        colorMap[ParticleType.PROJECTILE] = buildColor(
            assetService.gameConfig.particle.enemy.projectile.color.a.toFloat(),
            assetService.gameConfig.particle.enemy.projectile.color.r.toFloat(),
            assetService.gameConfig.particle.enemy.projectile.color.g.toFloat(),
            assetService.gameConfig.particle.enemy.projectile.color.b.toFloat()
        )
    }

    private fun buildColor(alpha: Float, red: Float, green: Float, blue: Float): Color {
        return Color(red, green, blue, alpha)
    }

}
