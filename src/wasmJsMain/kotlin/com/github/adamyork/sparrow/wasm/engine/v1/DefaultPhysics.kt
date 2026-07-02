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
        val adjustedBoundaries = if (player.y != nextY) {
            collision.recomputeXBoundaries(player.copy(y = nextY), collisionBoundaries)
        } else {
            collisionBoundaries
        }
        val leftEdge = minOf(adjustedBoundaries.left + 1, adjustedBoundaries.right - 1)
        val rightEdge = maxOf(adjustedBoundaries.left + 1, adjustedBoundaries.right - 1)
        val minBound = maxOf(0, leftEdge)
        val deltaX = velocityX * physicsSettingsService.xMovementDistance * deltaTime
        val nextX = (player.x + deltaX).roundToInt().coerceIn(minBound, rightEdge)
        return player.copy(
            x = nextX,
            vx = velocityX,
            y = nextY,
            vy = if (nextJumping == PlayerJumpingState.GROUNDED) 0.0 else velocityY,
            jumping = nextJumping,
            colliding = if (isColliding && velocityX == 0.0) GameElementCollisionState.FREE else player.colliding
        )
    }

    override fun applyPlayerCollisionPhysics(player: Player, rect: Rect?, viewPort: ViewPort): Player {
        val enemyRect = rect ?: return player
        val playerCenterX = player.x + (player.width / 2)
        val enemyCenterX = enemyRect.left + (enemyRect.width / 2)
        val knockbackDirection = if (playerCenterX < enemyCenterX) -1.0 else 1.0
        val knockbackStrength = 15.0
        return player.copy(
            vx = knockbackStrength * knockbackDirection,
            colliding = GameElementCollisionState.COLLIDING
        )
    }

    override fun applyCollisionParticlePhysics(
        mapParticles: ArrayList<Particle>,
        viewPort: ViewPort
    ): ArrayList<Particle> {
        val dt = statusProvider.getDeltaTimeCoefficient()
        // Lower this value to slow down the animation (e.g., 0.5 is half-speed)
        val speedFactor = 0.25

        return mapParticles
            .map { particle ->
                if (particle.type == ParticleType.COLLISION) {
                    // Slower frame progression
                    val nextFrame = particle.frame + (1.0 * dt * speedFactor).toInt().coerceAtLeast(1)

                    var nextRadius = particle.radius
                    var position = Pair(particle.x.toDouble(), particle.y.toDouble())

                    if (particle.radius < DefaultParticles.MAX_SQUARE_RADIAL_RADIUS) {
                        // Slower radius expansion (change 10 to a smaller value if needed)
                        nextRadius = (particle.radius + (10 * dt * speedFactor)).toInt()

                        val pos = getCollisionParticlePosition(
                            nextRadius.toFloat(),
                            particle.id.toFloat(),
                            particle.originX,
                            particle.originY
                        )
                        position = Pair(pos.first.toDouble(), pos.second.toDouble())
                    } else {
                        if (particle.frame <= particle.lifetime) {
                            // Slower gravity effect
                            position = Pair(
                                particle.x.toDouble(),
                                particle.y.toDouble() + (physicsSettingsService.gravity * dt * speedFactor)
                            )
                        }
                    }
                    particle.copy(
                        x = position.first.toInt() + particle.xJitter,
                        y = position.second.toInt() + particle.yJitter,
                        frame = nextFrame,
                        radius = nextRadius
                    )
                } else {
                    particle
                }
            }
            .filter { particle ->
                if (particle.type == ParticleType.COLLISION) {
                    particle.isActiveVisibleCollisionParticle(viewPort)
                } else {
                    true
                }
            }
            .toCollection(ArrayList())
    }

    override fun applyDustParticlePhysics(mapParticles: ArrayList<Particle>): ArrayList<Particle> {
        val dt = statusProvider.getDeltaTimeCoefficient()
        return mapParticles.map { p ->
            if (p.type == ParticleType.DUST) {
                val growth = 1.0 * dt
                p.copy(
                    width = (p.width + growth).toInt().coerceAtMost(40),
                    height = (p.height + growth).toInt().coerceAtMost(40),
                    frame = p.frame + 1
                )
            } else {
                p
            }
        }.filter { it.frame <= it.lifetime }.toCollection(ArrayList())
    }

    override fun applyProjectileParticlePhysics(
        mapParticles: ArrayList<Particle>,
        viewPort: ViewPort
    ): ArrayList<Particle> {
        val dt = statusProvider.getDeltaTimeCoefficient()
        val speed = physicsSettingsService.projectileSpeed * dt
        return mapParticles.map { p ->
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
                p.copy(x = nextX.roundToInt(), y = nextY.roundToInt(), frame = p.frame + 1)
            } else {
                p
            }
        }.filter { particle ->
            if (particle.type == ParticleType.PROJECTILE) {
                isParticleInViewPort(particle, viewPort)
            } else {
                particle.frame <= particle.lifetime
            }
        }.toCollection(ArrayList())
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

    override fun applyMapItemReturnParticlePhysics(mapParticles: ArrayList<Particle>): ArrayList<Particle> {
        return mapParticles
            .map { particle ->
                if (particle.type == ParticleType.MAP_ITEM_RETURN) {
                    val nextFrame = particle.frame + 1
                    val step = nextFrame.toFloat() / (particle.lifetime - 1)
                    val angle = PI * (1 + step)
                    val nextX = particle.originX + cos(angle).toFloat() * 90
                    val nextY = particle.originY + sin(angle).toFloat() * 90
                    particle.copy(x = nextX.toInt(), y = nextY.toInt(), frame = nextFrame)
                } else {
                    particle
                }
            }.filter { particle -> particle.frame <= particle.lifetime }
            .toCollection(ArrayList())
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
