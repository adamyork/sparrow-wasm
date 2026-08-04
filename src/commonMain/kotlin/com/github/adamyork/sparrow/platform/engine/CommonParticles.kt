package com.github.adamyork.sparrow.platform.engine

import androidx.compose.ui.graphics.Color
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.data.Direction
import com.github.adamyork.sparrow.platform.common.data.enemy.Enemy
import com.github.adamyork.sparrow.platform.common.data.item.Item
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.engine.data.Particle
import com.github.adamyork.sparrow.platform.engine.data.ParticleShape
import com.github.adamyork.sparrow.platform.engine.data.ParticleType
import com.github.adamyork.sparrow.platform.service.AssetService
import me.tatarka.inject.annotations.Inject
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class CommonParticles : Particles {

    companion object {
        const val MAX_SQUARE_RADIAL_RADIUS: Int = 45
        const val GPU_COMPUTE_FLOATS_PER_PARTICLE: Int = 16
        const val DEFAULT_GPU_PARTICLE_CAPACITY: Int = 4096
        private const val MAX_ACTIVE_PROJECTILES: Int = 1
        private const val MAX_ACTIVE_MAP_ITEM_RETURN_PARTICLES: Int = 1
        private const val COLLISION_PARTICLE_COUNT: Int = 1024
        private const val PROJECTILE_SIZE: Int = 24
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

    override fun applyCollisionParticles(originX: Int, originY: Int, particles: ArrayList<Particle>) {
        val collisionColor = colorMap[ParticleType.COLLISION] ?: Color.White
        repeat(COLLISION_PARTICLE_COUNT) { index ->
            particles.add(
                Particle(
                    index,
                    originX,
                    originY,
                    originX,
                    originY,
                    8,
                    8,
                    ParticleType.COLLISION,
                    0,
                    40,
                    Random.nextInt(50),
                    Random.nextInt(50),
                    1,
                    collisionColor,
                    ParticleShape.RECT
                )
            )
        }
    }

    override fun applyDustParticles(player: Player, particles: ArrayList<Particle>) {
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
            particles.add(
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
    }

    override fun applyProjectileParticle(
        player: Player,
        enemy: Enemy,
        particles: ArrayList<Particle>
    ): Boolean {
        val count = getActiveProjectileCount(particles)
        if (count >= MAX_ACTIVE_PROJECTILES) return false
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
        return true
    }

    override fun applyMapItemReturnParticle(player: Player, mapItem: Item, particles: ArrayList<Particle>) {
        particles.add(
            Particle(
                0,
                player.x,
                player.y,
                mapItem.x,
                mapItem.y,
                mapItem.width,
                mapItem.height,
                ParticleType.MAP_ITEM_RETURN,
                0,
                500,
                0,
                0,
                1,
                Color.White,
                ParticleShape.RECT
            )
        )
    }

    override fun populateColorMap(assetService: AssetService) {
        colorMap = mapOf(
            ParticleType.DUST to Color(
                assetService.appProperties.particle.player.movement.color.r.toFloat() / 255f,
                assetService.appProperties.particle.player.movement.color.g.toFloat() / 255f,
                assetService.appProperties.particle.player.movement.color.b.toFloat() / 255f,
                assetService.appProperties.particle.player.movement.color.a.toFloat() / 255f
            ),
            ParticleType.COLLISION to Color(
                assetService.appProperties.particle.player.collision.color.r.toFloat() / 255f,
                assetService.appProperties.particle.player.collision.color.g.toFloat() / 255f,
                assetService.appProperties.particle.player.collision.color.b.toFloat() / 255f,
                assetService.appProperties.particle.player.collision.color.a.toFloat() / 255f
            ),
            ParticleType.PROJECTILE to Color(
                assetService.appProperties.particle.enemy.projectile.color.r.toFloat() / 255f,
                assetService.appProperties.particle.enemy.projectile.color.g.toFloat() / 255f,
                assetService.appProperties.particle.enemy.projectile.color.b.toFloat() / 255f,
                assetService.appProperties.particle.enemy.projectile.color.a.toFloat() / 255f
            )
        )
    }

    fun createGpuParticleComputeBuffer(maxParticles: Int = DEFAULT_GPU_PARTICLE_CAPACITY): FloatArray {
        return FloatArray(maxParticles * GPU_COMPUTE_FLOATS_PER_PARTICLE)
    }

    fun writeGpuParticleSpawnBuffer(
        mapParticles: List<Particle>,
        targetBuffer: FloatArray,
        maxParticles: Int = DEFAULT_GPU_PARTICLE_CAPACITY,
        startSlot: Int = 0
    ): Int {
        var activeCount = 0
        val clampedMaxParticles = maxParticles.coerceAtLeast(0)
        val floatsPerParticle = GPU_COMPUTE_FLOATS_PER_PARTICLE
        if (clampedMaxParticles == 0) return 0

        var clearIndex = 0
        val maxFloatCount = clampedMaxParticles * floatsPerParticle
        while (clearIndex < maxFloatCount && clearIndex < targetBuffer.size) {
            targetBuffer[clearIndex++] = 0f
        }

        val reservedProjectileSlots = MAX_ACTIVE_PROJECTILES.coerceAtMost(clampedMaxParticles)
        val remainingAfterProjectile = (clampedMaxParticles - reservedProjectileSlots).coerceAtLeast(0)
        val reservedMapItemReturnSlots =
            MAX_ACTIVE_MAP_ITEM_RETURN_PARTICLES.coerceAtMost(remainingAfterProjectile)
        val firstMapItemReturnSlot = clampedMaxParticles - reservedMapItemReturnSlots
        val firstProjectileSlot = firstMapItemReturnSlot - reservedProjectileSlots
        val ringBufferCapacity = firstProjectileSlot.coerceAtLeast(0)
        var slot = if (ringBufferCapacity > 0) startSlot.mod(ringBufferCapacity) else 0
        for (particle in mapParticles) {
            if (activeCount >= clampedMaxParticles) break
            if (
                particle.type != ParticleType.COLLISION &&
                particle.type != ParticleType.DUST &&
                particle.type != ParticleType.PROJECTILE &&
                particle.type != ParticleType.MAP_ITEM_RETURN
            ) continue

            val isProjectile = particle.type == ParticleType.PROJECTILE
            val isMapItemReturn = particle.type == ParticleType.MAP_ITEM_RETURN
            val slotIndex = if (isProjectile && reservedProjectileSlots > 0) {
                val projectileSlotOffset = (particle.id - 1).coerceAtLeast(0) % reservedProjectileSlots
                firstProjectileSlot + projectileSlotOffset
            } else if (isMapItemReturn && reservedMapItemReturnSlots > 0) {
                val mapItemReturnSlotOffset = (particle.id - 1).coerceAtLeast(0) % reservedMapItemReturnSlots
                firstMapItemReturnSlot + mapItemReturnSlotOffset
            } else {
                slot
            }
            val baseIndex = slotIndex * floatsPerParticle
            if (baseIndex + (floatsPerParticle - 1) >= targetBuffer.size) {
                break
            }

            val isDust = particle.type == ParticleType.DUST
            val angle = particle.id.toFloat() * (PI.toFloat() / 180f)
            val baseVelocity = 70f
            val jitterScale = 0.8f
            val projectileDirectionX = (particle.originX - particle.xJitter).toFloat()
            val projectileDirectionY = (particle.originY - particle.yJitter).toFloat()
            val projectileLength = sqrt((projectileDirectionX * projectileDirectionX) + (projectileDirectionY * projectileDirectionY))
            val projectileUnitX = if (projectileLength > 0f) projectileDirectionX / projectileLength else 1f
            val projectileUnitY = if (projectileLength > 0f) projectileDirectionY / projectileLength else 0f
            val mapItemTargetX = particle.originX.toFloat()
            val mapItemTargetY = particle.originY.toFloat()
            val velocityX = when {
                isDust -> 0f
                isProjectile -> projectileUnitX
                isMapItemReturn -> mapItemTargetX
                else -> (cos(angle) * baseVelocity) + (particle.xJitter - 25f) * jitterScale
            }
            val velocityY = when {
                isDust -> 0f
                isProjectile -> projectileUnitY
                isMapItemReturn -> mapItemTargetY
                else -> (sin(angle) * baseVelocity) + (particle.yJitter - 25f) * jitterScale
            }
            val size = max(particle.width, particle.height).toFloat()
            val spriteWidth = particle.width.toFloat().coerceAtLeast(1f)
            val spriteHeight = particle.height.toFloat().coerceAtLeast(1f)
            val usesCenterAnchor = particle.shape == ParticleShape.CIRCLE
            val spawnX = if (usesCenterAnchor) particle.x.toFloat() + (size * 0.5f) else particle.x.toFloat()
            val spawnY = if (usesCenterAnchor) particle.y.toFloat() + (size * 0.5f) else particle.y.toFloat()

            var writeIndex = baseIndex
            targetBuffer[writeIndex++] = spawnX
            targetBuffer[writeIndex++] = spawnY
            targetBuffer[writeIndex++] = velocityX
            targetBuffer[writeIndex++] = velocityY
            targetBuffer[writeIndex++] = particle.frame.toFloat().coerceAtLeast(0f)
            targetBuffer[writeIndex++] = particle.lifetime.toFloat().coerceAtLeast(1f)
            targetBuffer[writeIndex++] = if (isMapItemReturn) spriteWidth else size
            targetBuffer[writeIndex++] = 1f
            targetBuffer[writeIndex++] = particle.color.red.coerceIn(0f, 1f)
            targetBuffer[writeIndex++] = particle.color.green.coerceIn(0f, 1f)
            targetBuffer[writeIndex++] = particle.color.blue.coerceIn(0f, 1f)
            targetBuffer[writeIndex++] = particle.color.alpha.coerceIn(0f, 1f)
            val particleKind = when (particle.type) {
                ParticleType.DUST -> 1f
                ParticleType.PROJECTILE -> 2f
                ParticleType.MAP_ITEM_RETURN -> 3f
                else -> 0f
            }
            targetBuffer[writeIndex++] = particleKind
            targetBuffer[writeIndex++] = if (particle.shape == ParticleShape.CIRCLE) 1f else 0f
            targetBuffer[writeIndex++] = if (isMapItemReturn) spriteHeight else 0f
            targetBuffer[writeIndex] = 0f
            activeCount++
            if (!isProjectile && !isMapItemReturn && ringBufferCapacity > 0) {
                slot = (slot + 1) % ringBufferCapacity
            }
        }
        return activeCount
    }

    private fun getActiveProjectileCount(particles: ArrayList<Particle>): Int {
        var count = 0
        for (particleIndex in particles.indices) {
            if (particles[particleIndex].type == ParticleType.PROJECTILE) {
                count++
            }
        }
        return count
    }

}
