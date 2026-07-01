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
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.PhysicsSettingsService
import me.tatarka.inject.annotations.Inject
import kotlin.math.*

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class DefaultPhysics @AppScope @Inject constructor(
    private val statusProviderFactory: () -> DefaultStatusProvider,
    val physicsSettingsService: PhysicsSettingsService,
    private val assetService: AssetService
) : Physics {

    private val statusProvider: DefaultStatusProvider
        get() = statusProviderFactory()

    private val maxFps: Double
        get() = assetService.gameConfig.engine.fps.max.toDouble()

    override fun applyPlayerPhysics(
        player: Player,
        collisionBoundaries: CollisionBoundaries,
        collision: Collision
    ): Player {
        val dt = statusProvider.getDeltaTimeCoefficient()
        val vx = getXVelocity(player.vx, player.moving, dt)
        val vy = getYVelocity(player.vy, player.jumping, dt)

        val yResult = movePlayerY(player.y, vy, player.jumping, collisionBoundaries, dt)
        var adjustedBoundaries = collisionBoundaries
        if (player.y != yResult.y) {
            adjustedBoundaries = collision.recomputeXBoundaries(player.copy(y = yResult.y), collisionBoundaries)
        }

        val xResult = movePlayerX(player.x, vx, player.moving, player.direction, adjustedBoundaries, dt)
        return player.copy(x = xResult.x, vx = xResult.vx, y = yResult.y, vy = yResult.vy, jumping = yResult.jumping)
    }

    override fun applyPlayerCollisionPhysics(
        player: Player,
        rect: Rect?,
        viewPort: ViewPort
    ): Player {
        val enemyRect = rect ?: return player
        val playerCenterX = player.x + (player.width / 2)
        val enemyCenterX = enemyRect.left + (enemyRect.width / 2)
        val rawNextX = if (playerCenterX < enemyCenterX) {
            enemyRect.left.toInt() - player.width
        } else {
            enemyRect.right.toInt()
        }
        val minX = 0
        // Clamp in world coordinates; viewport width alone is local-space and causes large snaps.
        val maxX = (viewPort.x + viewPort.width - player.width).coerceAtLeast(minX)
        val clampedNextX = rawNextX.coerceIn(minX, maxX)
        return player.copy(
            x = clampedNextX,
            vx = 0.0,
            colliding = GameElementCollisionState.COLLIDING
        )
    }

    private fun getXVelocity(vx: Double, moving: PlayerMovingState, dt: Double): Double {
        var nextVx = vx
        if (moving == PlayerMovingState.MOVING) {
            // Accelerate
            val accel = physicsSettingsService.xAccelerationRate * dt * maxFps
            nextVx += accel
            // Cap speed
            if (nextVx > physicsSettingsService.maxXVelocity) {
                nextVx = physicsSettingsService.maxXVelocity
            }
        } else {
            // Friction decay
            val frictionDecay = physicsSettingsService.friction.pow(dt * maxFps)
            nextVx *= frictionDecay
        }
        // Stop threshold
        if (moving != PlayerMovingState.MOVING && abs(nextVx) < 0.5) {
            nextVx = 0.0
        }
        return nextVx
    }

    private fun getYVelocity(vy: Double, jumping: PlayerJumpingState, dt: Double): Double {
        return when (jumping) {
            PlayerJumpingState.INITIAL -> physicsSettingsService.jumpDistance / 2.0
            PlayerJumpingState.RISING -> (vy + (physicsSettingsService.yVelocityCoefficient * vy * dt * maxFps)).coerceAtMost(
                physicsSettingsService.maxYVelocity
            )

            else -> 0.0
        }
    }

    private fun movePlayerX(x: Int, vx: Double, moving: PlayerMovingState, dir: Direction, b: CollisionBoundaries, dt: Double): PhysicsXResult {
        // VX is now pixels-per-frame-equivalent
        val delta = vx // Keep it as vx, the acceleration is already handled in getXVelocity

        var nextX = x.toDouble() + (if (dir == Direction.LEFT) -delta else delta)

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
        val gravityEffect = physicsSettingsService.gravity * dt * maxFps
        var nextY = y + gravityEffect - (vy * dt * maxFps)
        var nextJumping = jumping

        if (nextJumping == PlayerJumpingState.INITIAL) nextJumping = PlayerJumpingState.RISING
        if (nextJumping == PlayerJumpingState.RISING && nextY <= (b.bottom - physicsSettingsService.jumpDistance)) {
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
        return mapParticles
            .map { particle ->
                if (particle.type == ParticleType.COLLISION) {
                    val nextFrame = particle.frame + 1
                    var nextRadius = particle.radius
                    var position = Pair(particle.x.toDouble(), particle.y.toDouble())
                    if (particle.radius < DefaultParticles.MAX_SQUARE_RADIAL_RADIUS) {
                        nextRadius = particle.radius + 10
                        val pos = getCollisionParticlePosition(
                            nextRadius.toFloat(),
                            particle.id.toFloat(),
                            particle.originX,
                            particle.originY
                        )
                        position = Pair(pos.first.toDouble(), pos.second.toDouble())
                    } else {
                        if (particle.frame <= particle.lifetime) {
                            position = Pair(
                                particle.x.toDouble(),
                                particle.y.toDouble() + physicsSettingsService.gravity
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
            val growth = 1.0 * dt * maxFps
            p.copy(
                width = (p.width + growth).toInt().coerceAtMost(40),
                height = (p.height + growth).toInt().coerceAtMost(40),
                frame = p.frame + 1
            )
        }.filter { it.frame <= it.lifetime }.toCollection(ArrayList())
    }

    override fun applyProjectileParticlePhysics(mapParticles: ArrayList<Particle>): ArrayList<Particle> {
        val dt = statusProvider.getDeltaTimeCoefficient()
        return mapParticles.map { p ->
            val speed = 5.0 * dt * maxFps// Adjust based on desired projectile speed
            val dx = if (p.originX < p.x) -speed else speed
            val dy = if (p.originY < p.y) -speed else speed
            p.copy(x = (p.x + dx).toInt(), y = (p.y + dy).toInt(), frame = p.frame + 1)
        }.filter { it.frame <= it.lifetime }.toCollection(ArrayList())
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
