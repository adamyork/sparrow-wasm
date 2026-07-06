package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.data.Direction
import com.github.adamyork.sparrow.wasm.common.data.GameElementCollisionState
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.common.data.player.PlayerJumpingState
import com.github.adamyork.sparrow.wasm.common.data.player.PlayerMovingState
import com.github.adamyork.sparrow.wasm.common.v1.DefaultStatusProvider
import com.github.adamyork.sparrow.wasm.engine.Collision
import com.github.adamyork.sparrow.wasm.engine.Physics
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.engine.data.Particle
import com.github.adamyork.sparrow.wasm.engine.data.ParticleType
import com.github.adamyork.sparrow.wasm.service.PhysicsSettingsService
import me.tatarka.inject.annotations.Inject
import kotlin.math.*

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class DefaultPhysics @AppScope @Inject constructor(
    private val statusProviderFactory: () -> DefaultStatusProvider,
    val physicsSettingsService: PhysicsSettingsService
) : Physics {

    private val statusProvider: DefaultStatusProvider
        get() = statusProviderFactory()

    override fun applyPlayerPhysics(
        player: Player,
        collisionBoundaries: CollisionBoundaries,
        collision: Collision
    ): Player {
        val deltaTime = statusProvider.getDeltaTimeCoefficient()
        val isColliding = player.colliding == GameElementCollisionState.COLLIDING
        var velocityX = if (isColliding) {
            player.vx * 0.85.pow(deltaTime)
        } else {
            when (player.moving) {
                PlayerMovingState.MOVING -> {
                    val directionModifier = if (player.direction == Direction.LEFT) -1.0 else 1.0
                    val acceleration = physicsSettingsService.xAccelerationRate * deltaTime * directionModifier
                    (player.vx + acceleration).coerceIn(
                        -physicsSettingsService.maxXVelocity,
                        physicsSettingsService.maxXVelocity
                    )
                }

                else -> {
                    val frictionDecay = physicsSettingsService.friction.coerceIn(0.0, 1.0).pow(deltaTime)
                    player.vx * frictionDecay
                }
            }
        }
        if (abs(velocityX) < 0.5) velocityX = 0.0
        val initialJumpVelocity = min(physicsSettingsService.jumpDistance / 8.0, physicsSettingsService.maxYVelocity)
        val accelerationAdjustment = physicsSettingsService.yVelocityCoefficient * player.vy * deltaTime
        val boundedAndAdjustedVelocityY =
            (player.vy + accelerationAdjustment).coerceAtMost(physicsSettingsService.maxYVelocity)
        val velocityY = when (player.jumping) {
            PlayerJumpingState.INITIAL -> initialJumpVelocity
            PlayerJumpingState.RISING -> boundedAndAdjustedVelocityY
            else -> 0.0
        }
        val gravityEffect = physicsSettingsService.gravity * deltaTime
        val nextY = (player.y + gravityEffect - (velocityY * deltaTime)).roundToInt()
            .coerceIn(collisionBoundaries.top, collisionBoundaries.bottom)
        val reachedCeiling = nextY <= collisionBoundaries.top
        val reachedJumpApex =
            player.jumping == PlayerJumpingState.RISING && nextY <= (collisionBoundaries.bottom - physicsSettingsService.jumpDistance)
        val hitGround = nextY >= collisionBoundaries.bottom
        val nextJumping = when {
            hitGround -> PlayerJumpingState.GROUNDED
            reachedCeiling || reachedJumpApex -> PlayerJumpingState.HEIGHT_REACHED
            player.jumping == PlayerJumpingState.INITIAL -> PlayerJumpingState.RISING
            else -> player.jumping
        }
        if (player.y != nextY) {
            player.y = nextY
            collision.updateCollisionXBoundaries(player, collisionBoundaries)
        }
        val safeLeft = min(collisionBoundaries.left, collisionBoundaries.right)
        val safeRight = max(collisionBoundaries.left, collisionBoundaries.right)
        val minBound = max(0, safeLeft)
        val deltaX = velocityX * physicsSettingsService.xMovementDistance * deltaTime
        val nextX = (player.x + deltaX).roundToInt().coerceIn(minBound, safeRight)
        val nextImmunityTicks = (player.immunityTicks - 1).coerceAtLeast(0)
        player.x = nextX
        player.vx = velocityX
        player.y = nextY
        player.vy = if (nextJumping == PlayerJumpingState.GROUNDED) 0.0 else velocityY
        player.jumping = nextJumping
        player.immunityTicks = nextImmunityTicks
        player.colliding = if (isColliding && velocityX == 0.0 && nextImmunityTicks <= 0)
            GameElementCollisionState.FREE
        else player.colliding
        return player
    }

    override fun applyPlayerCollisionPhysics(player: Player, rect: Rect?, viewPort: ViewPort): Player {
        val enemyRect = rect ?: return player
        val playerCenterX = player.x + (player.width / 2)
        val enemyCenterX = enemyRect.left + (enemyRect.width / 2)
        val knockbackDirection = if (playerCenterX < enemyCenterX) -1.0 else 1.0
        val knockbackStrength = 15.0
        val projectedVx = knockbackStrength * knockbackDirection
        val projectedX = (player.x + projectedVx).toInt()
        val effectiveWidthMultiplier = if (player.direction == Direction.LEFT) 2 else 1
        val minBound = viewPort.x
        val maxBound = (viewPort.x + viewPort.width) - (player.width * effectiveWidthMultiplier)
        val clampedX = projectedX.coerceIn(minBound, maxBound)
        val finalVx = if (projectedX != clampedX) 0.0 else projectedVx
        player.x = clampedX
        player.vx = finalVx
        player.colliding = GameElementCollisionState.COLLIDING
        player.immunityTicks = Player.IMMUNITY_TICKS_ON_HIT
        player.animationTickCounter = 0
        player.animationTickBufferMs = 0.0
        return player
    }

    override fun applyCollisionParticlePhysics(
        mapParticles: ArrayList<Particle>,
        viewPort: ViewPort
    ) {
        val dt = statusProvider.getDeltaTimeCoefficient()
        val speedFactor = 0.25
        for (i in mapParticles.indices.reversed()) {
            val p = mapParticles[i]
            if (p.type == ParticleType.COLLISION) {
                val nextFrame = p.frame + (1.0 * dt * speedFactor).toInt().coerceAtLeast(1)
                var nextRadius = p.radius
                var positionX = p.x.toDouble()
                var positionY = p.y.toDouble()
                if (p.radius < DefaultParticles.MAX_SQUARE_RADIAL_RADIUS) {
                    nextRadius = (p.radius + (10 * dt * speedFactor)).toInt()
                    val pos = getCollisionParticlePosition(
                        nextRadius.toFloat(),
                        p.id.toFloat(),
                        p.originX,
                        p.originY
                    )
                    positionX = pos.first.toDouble()
                    positionY = pos.second.toDouble()
                } else {
                    if (p.frame <= p.lifetime) {
                        positionY += (physicsSettingsService.gravity * dt * speedFactor)
                    }
                }
                val updated = p.copy(
                    x = positionX.toInt() + p.xJitter,
                    y = positionY.toInt() + p.yJitter,
                    frame = nextFrame,
                    radius = nextRadius
                )
                if (!updated.isActiveVisibleCollisionParticle(viewPort)) {
                    mapParticles.removeAt(i)
                } else {
                    mapParticles[i] = updated
                }
            }
        }
    }

    override fun applyDustParticlePhysics(mapParticles: ArrayList<Particle>) {
        val dt = statusProvider.getDeltaTimeCoefficient()
        for (i in mapParticles.size - 1 downTo 0) {
            val p = mapParticles[i]
            if (p.type == ParticleType.DUST) {
                if (p.frame >= p.lifetime) {
                    mapParticles.removeAt(i)
                } else {
                    val growth = 1.0 * dt
                    mapParticles[i] = p.copy(
                        width = (p.width + growth).toInt().coerceAtMost(40),
                        height = (p.height + growth).toInt().coerceAtMost(40),
                        frame = p.frame + 1
                    )
                }
            }
        }
    }

    override fun applyProjectileParticlePhysics(
        mapParticles: ArrayList<Particle>,
        viewPort: ViewPort
    ) {
        val dt = statusProvider.getDeltaTimeCoefficient()
        val speed = physicsSettingsService.projectileSpeed * dt
        for (i in mapParticles.indices.reversed()) {
            val p = mapParticles[i]
            if (p.type == ParticleType.PROJECTILE) {
                val directionX = p.originX - p.xJitter
                val directionY = p.originY - p.yJitter
                val length = sqrt((directionX * directionX + directionY * directionY).toDouble())
                val unitVector = if (length > 0.0) {
                    Pair(directionX / length, directionY / length)
                } else {
                    Pair(1.0, 0.0)
                }
                val nextX = p.x + (unitVector.first * speed)
                val nextY = p.y + (unitVector.second * speed)
                val updated = p.copy(x = nextX.roundToInt(), y = nextY.roundToInt(), frame = p.frame + 1)
                if (!isParticleInViewPort(updated, viewPort)) {
                    mapParticles.removeAt(i)
                } else {
                    mapParticles[i] = updated
                }
            } else {
                if (p.frame > p.lifetime) {
                    mapParticles.removeAt(i)
                }
            }
        }
    }

    private fun isParticleInViewPort(particle: Particle, viewPort: ViewPort): Boolean {
        val left = viewPort.x
        val right = viewPort.x + viewPort.width
        val top = viewPort.y
        val bottom = viewPort.y + viewPort.height
        val particleRight = particle.x + particle.width
        val particleBottom = particle.y + particle.height
        return particleRight >= left && particle.x <= right && particleBottom >= top && particle.y <= bottom
    }

    override fun applyMapItemReturnParticlePhysics(mapParticles: ArrayList<Particle>) {
        for (i in mapParticles.indices.reversed()) {
            val p = mapParticles[i]
            if (p.type == ParticleType.MAP_ITEM_RETURN) {
                val nextFrame = p.frame + 1
                if (nextFrame > p.lifetime) {
                    mapParticles.removeAt(i)
                } else {
                    val step = nextFrame.toFloat() / (p.lifetime - 1)
                    val angle = PI * (1 + step)
                    val nextX = p.originX + cos(angle).toFloat() * 90
                    val nextY = p.originY + sin(angle).toFloat() * 90
                    mapParticles[i] = p.copy(x = nextX.toInt(), y = nextY.toInt(), frame = nextFrame)
                }
            } else {
                if (p.frame > p.lifetime) {
                    mapParticles.removeAt(i)
                }
            }
        }
    }

    private fun getCollisionParticlePosition(
        radius: Float,
        angleInDegrees: Float,
        originX: Int,
        originY: Int
    ): Pair<Float, Float> {
        val x: Float = (radius * cos(angleInDegrees * PI / 180f)).toFloat() + originX
        val y: Float = (radius * sin(angleInDegrees * PI / 180f)).toFloat() + originY
        return Pair(x, y)
    }
}
