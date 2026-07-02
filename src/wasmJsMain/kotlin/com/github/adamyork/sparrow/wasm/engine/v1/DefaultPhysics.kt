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
import com.github.adamyork.sparrow.wasm.engine.data.*
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
        val dt = statusProvider.getDeltaTimeCoefficient()
        val vx = getXVelocity(player.colliding == GameElementCollisionState.COLLIDING, player.vx, player.moving, dt)
        val vy = getYVelocity(player.vy, player.jumping, dt)

        val yResult = movePlayerY(player.y, vy, player.jumping, collisionBoundaries, dt)
        var adjustedBoundaries = collisionBoundaries
        if (player.y != yResult.y) {
            adjustedBoundaries = collision.recomputeXBoundaries(player.copy(y = yResult.y), collisionBoundaries)
        }

        val xResult = movePlayerX(player.x, vx, player.moving, player.direction, adjustedBoundaries, dt)
        return player.copy(x = xResult.x, vx = xResult.vx, y = yResult.y, vy = yResult.vy, jumping = yResult.jumping)
    }

    override fun applyPlayerCollisionPhysics(player: Player, rect: Rect?, viewPort: ViewPort): Player {
        val enemyRect = rect ?: return player
        val playerCenterX = player.x + (player.width / 2)
        val enemyCenterX = enemyRect.left + (enemyRect.width / 2)
        val knockbackDirection = if (playerCenterX < enemyCenterX) -1.0 else 1.0
        val knockbackStrength = player.width
        return player.copy(
            vx = knockbackStrength * knockbackDirection,
            colliding = GameElementCollisionState.COLLIDING
        )
    }

    private fun getXVelocity(isColliding: Boolean, vx: Double, moving: PlayerMovingState, dt: Double): Double {
        if (isColliding) return vx
        var nextVx = vx
        if (moving == PlayerMovingState.MOVING) {
            // Acceleration is tuned as per-frame units; only scale for dropped-frame compensation.
            val accel = physicsSettingsService.xAccelerationRate * dt
            nextVx += accel
            // Cap speed
            if (nextVx > physicsSettingsService.maxXVelocity) {
                nextVx = physicsSettingsService.maxXVelocity
            }
        } else {
            // Apply damping and linear deceleration while idle for smoother stop behavior.
            val frictionDecay = physicsSettingsService.friction.coerceIn(0.0, 1.0).pow(dt)
            nextVx *= frictionDecay
            nextVx = (nextVx - (physicsSettingsService.xDeaccelerationRate * dt)).coerceAtLeast(0.0)
        }
        // Stop threshold
        if (moving != PlayerMovingState.MOVING && abs(nextVx) < 0.5) {
            nextVx = 0.0
        }
        return nextVx
    }

    private fun getYVelocity(vy: Double, jumping: PlayerJumpingState, dt: Double): Double {
        return when (jumping) {
            PlayerJumpingState.INITIAL -> min(
                physicsSettingsService.jumpDistance / 2.0,
                physicsSettingsService.maxYVelocity
            )

            PlayerJumpingState.RISING -> (vy + (physicsSettingsService.yVelocityCoefficient * vy * dt)).coerceAtMost(
                physicsSettingsService.maxYVelocity
            )

            else -> 0.0
        }
    }

    private fun movePlayerX(
        x: Int,
        vx: Double,
        moving: PlayerMovingState,
        dir: Direction,
        b: CollisionBoundaries,
        dt: Double
    ): PhysicsXResult {
        val delta = vx * physicsSettingsService.xMovementDistance * dt

        val nextX = x.toDouble() + (if (dir == Direction.LEFT) -delta else delta)

        val clampedX = nextX.roundToInt().coerceIn(minOf(b.left + 1, b.right - 1), maxOf(b.left + 1, b.right - 1))
        return PhysicsXResult(clampedX, vx, moving)
    }

    private fun movePlayerY(
        y: Int,
        vy: Double,
        jumping: PlayerJumpingState,
        b: CollisionBoundaries,
        dt: Double
    ): PhysicsYResult {
        val gravityEffect = physicsSettingsService.gravity * dt
        var nextY = y + gravityEffect - (vy * dt)
        var nextJumping = jumping

        if (nextJumping == PlayerJumpingState.INITIAL) nextJumping = PlayerJumpingState.RISING
        if (nextJumping == PlayerJumpingState.RISING && nextY <= (b.bottom - physicsSettingsService.jumpDistance)) {
            nextJumping = PlayerJumpingState.HEIGHT_REACHED
        }

        if (nextY <= b.top) {
            nextY = b.top.toDouble()
            nextJumping = PlayerJumpingState.HEIGHT_REACHED
        }

        if (nextY >= b.bottom) {
            nextY = b.bottom.toDouble()
            nextJumping = PlayerJumpingState.GROUNDED
        }
        return PhysicsYResult(nextY.roundToInt(), vy, nextJumping)
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
