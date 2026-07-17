package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.data.ControlAction
import com.github.adamyork.sparrow.wasm.common.data.Direction
import com.github.adamyork.sparrow.wasm.common.data.GameElementCollisionState
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.common.data.player.PlayerJumpingState
import com.github.adamyork.sparrow.wasm.common.data.player.PlayerMovingState
import com.github.adamyork.sparrow.wasm.service.v1.DefaultRuntimeService
import com.github.adamyork.sparrow.wasm.engine.Collision
import com.github.adamyork.sparrow.wasm.engine.Physics
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.engine.data.Particle
import com.github.adamyork.sparrow.wasm.engine.data.ParticleType
import com.github.adamyork.sparrow.wasm.engine.data.PlayerPhysicsResult
import com.github.adamyork.sparrow.wasm.service.PhysicsSettingsService
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject
import kotlin.math.*

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultPhysics(
    private val statusProviderFactory: () -> DefaultRuntimeService,
    val physicsSettingsService: PhysicsSettingsService
) : Physics {

    private val logger = KotlinLogging.logger {}

    private val statusProvider: DefaultRuntimeService
        get() = statusProviderFactory()

    override fun applyPlayerPhysics(
        player: Player,
        collisionBoundaries: CollisionBoundaries,
        collision: Collision
    ) {
        val deltaTime = statusProvider.getDeltaTimeCoefficient()
        val isColliding = player.colliding == GameElementCollisionState.COLLIDING
        val nextImmunityTicks = (player.immunityTicks - 1).coerceAtLeast(0)
        val physicsResult = PlayerPhysicsResult()
        applyVerticalPhysics(player, collisionBoundaries, deltaTime, collision, physicsResult)
        applyHorizontalPhysics(player, collisionBoundaries, deltaTime, isColliding, physicsResult)
        player.x = physicsResult.nextX
        player.vx = physicsResult.velocityX
        player.y = physicsResult.nextY
        player.vy = physicsResult.velocityY
        player.jumping = physicsResult.nextJumping
        player.immunityTicks = nextImmunityTicks
        player.colliding = player.getNextCollidingState(physicsResult.velocityX, nextImmunityTicks)
    }

    private fun applyVerticalPhysics(
        player: Player,
        collisionBoundaries: CollisionBoundaries,
        deltaTime: Double,
        collision: Collision,
        physicsResult: PlayerPhysicsResult
    ) {
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
            .coerceIn(collisionBoundaries.minY, collisionBoundaries.maxY)
        val nextJumping = player.getNextJumpState(
            nextY = nextY,
            topBoundary = collisionBoundaries.top,
            bottomBoundary = collisionBoundaries.bottom,
            jumpDistance = physicsSettingsService.jumpDistance
        )
        physicsResult.nextY = nextY
        physicsResult.nextJumping = nextJumping
        physicsResult.velocityY = if (nextJumping == PlayerJumpingState.GROUNDED) 0.0 else velocityY
        if (player.y != nextY) {
            player.y = nextY
            collision.updateCollisionXBoundaries(player, collisionBoundaries)
        }
    }

    private fun applyHorizontalPhysics(
        player: Player,
        collisionBoundaries: CollisionBoundaries,
        deltaTime: Double,
        isColliding: Boolean,
        physicsResult: PlayerPhysicsResult
    ) {
        val targetVelocity = when {
            isColliding -> player.vx * physicsSettingsService.collisionVelocityDecay.pow(deltaTime)
            player.moving == PlayerMovingState.MOVING -> {
                val directionModifier = if (player.direction == Direction.LEFT) -1.0 else 1.0
                val acceleration = physicsSettingsService.xAccelerationRate * deltaTime * directionModifier
                (player.vx + acceleration).coerceIn(
                    -physicsSettingsService.maxXVelocity,
                    physicsSettingsService.maxXVelocity
                )
            }

            else -> player.vx * physicsSettingsService.friction.coerceIn(0.0, 1.0).pow(deltaTime)
        }
        val velocityX = if (abs(targetVelocity) < physicsSettingsService.minActiveVelocity) 0.0 else targetVelocity
        val deltaX = velocityX * physicsSettingsService.xMovementDistance * deltaTime
        val nextX = (player.x + deltaX).roundToInt().coerceIn(collisionBoundaries.minX, collisionBoundaries.maxX)
        physicsResult.nextX = nextX
        physicsResult.velocityX = velocityX
    }

    override fun applyPlayerCollisionPhysics(player: Player, rect: Rect?, viewPort: ViewPort) {
        val enemyRect = rect ?: return
        val playerCenterX = player.x + (player.width / 2)
        val enemyCenterX = enemyRect.left + (enemyRect.width / 2)
        val knockbackDirection = if (playerCenterX < enemyCenterX) -1.0 else 1.0
        val knockbackStrength = physicsSettingsService.collisionKnockbackStrength
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
    }

    override fun applyCollisionParticlePhysics(
        mapParticles: ArrayList<Particle>,
        viewPort: ViewPort
    ) {
        val deltaTimeCoefficient = statusProvider.getDeltaTimeCoefficient()
        val speed = physicsSettingsService.collisionParticleSpeedCoefficient
        for (particleIndex in mapParticles.indices.reversed()) {
            val particle = mapParticles[particleIndex]
            if (particle.type == ParticleType.COLLISION) {
                particle.frame += (1.0 * deltaTimeCoefficient * speed).toInt().coerceAtLeast(1)
                if (particle.radius < DefaultParticles.MAX_SQUARE_RADIAL_RADIUS) {
                    particle.radius =
                        (particle.radius + (physicsSettingsService.collisionParticleSizeMultiplier * deltaTimeCoefficient * speed)).toInt()
                    val pos = getCollisionParticlePosition(
                        particle.radius.toFloat(),
                        particle.id.toFloat(),
                        particle.originX,
                        particle.originY
                    )
                    particle.x = pos.first.toInt() + particle.xJitter
                    particle.y = pos.second.toInt() + particle.yJitter
                } else if (particle.frame <= particle.lifetime) {
                    particle.y += (physicsSettingsService.gravity * deltaTimeCoefficient * speed).toInt()
                }

                if (!particle.isActiveVisibleCollisionParticle(viewPort)) {
                    mapParticles.removeAt(particleIndex)
                }
            }
        }
    }

    override fun applyDustParticlePhysics(mapParticles: ArrayList<Particle>) {
        val deltaTimeCoefficient = statusProvider.getDeltaTimeCoefficient()
        val speed = physicsSettingsService.dustParticleSpeedCoefficient
        for (particleIndex in mapParticles.size - 1 downTo 0) {
            val particle = mapParticles[particleIndex]
            if (particle.type == ParticleType.DUST) {
                if (particle.frame >= particle.lifetime) {
                    mapParticles.removeAt(particleIndex)
                } else {
                    val growth = (1.0 * deltaTimeCoefficient * speed).toInt()
                    particle.width = (particle.width + growth).coerceAtMost(40)
                    particle.height = (particle.height + growth).coerceAtMost(40)
                    particle.frame += 1
                }
            }
        }
    }

    override fun applyProjectileParticlePhysics(
        mapParticles: ArrayList<Particle>,
        viewPort: ViewPort
    ) {
        val deltaTimeCoefficient = statusProvider.getDeltaTimeCoefficient()
        val speed = physicsSettingsService.projectileSpeed * deltaTimeCoefficient
        for (particleIndex in mapParticles.indices.reversed()) {
            val particle = mapParticles[particleIndex]
            if (particle.type == ParticleType.PROJECTILE) {
                val directionX = particle.originX - particle.xJitter
                val directionY = particle.originY - particle.yJitter
                val length = sqrt((directionX * directionX + directionY * directionY).toDouble())
                val unitVector = if (length > 0.0) {
                    Pair(directionX / length, directionY / length)
                } else {
                    Pair(1.0, 0.0)
                }
                particle.x += (unitVector.first * speed).roundToInt()
                particle.y += (unitVector.second * speed).roundToInt()
                particle.frame += 1

                if (!isParticleInViewPort(particle, viewPort)) {
                    mapParticles.removeAt(particleIndex)
                }
            } else if (particle.frame > particle.lifetime) {
                mapParticles.removeAt(particleIndex)
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

    override fun applyMapItemReturnParticlePhysics(mapParticles: ArrayList<Particle>, viewPort: ViewPort) {
        val deltaTimeCoefficient = statusProvider.getDeltaTimeCoefficient()
        val speed = physicsSettingsService.mapItemReturnParticleSpeed
        for (particleIndex in mapParticles.indices.reversed()) {
            val particle = mapParticles[particleIndex]
            if (particle.type == ParticleType.MAP_ITEM_RETURN) {
                particle.frame += 1
                val localCoords = viewPort.globalToLocal(particle.originX, particle.originY)
                val dx = localCoords.first - particle.x.toDouble()
                val dy = localCoords.second - particle.y.toDouble()
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < physicsSettingsService.mapItemReturnParticleMinTravelDist || particle.frame >= particle.lifetime) {
                    mapParticles.removeAt(particleIndex)
                } else {
                    val moveStep = speed * deltaTimeCoefficient
                    val ratio = moveStep / distance
                    particle.x += (dx * ratio).toInt()
                    particle.y += (dy * ratio).toInt()
                }
            } else if (particle.frame > particle.lifetime) {
                mapParticles.removeAt(particleIndex)
            }
        }
    }


    override fun changeXVelocityIfDirectionChanged(controlAction: ControlAction, player: Player) {
        val movingLeft = controlAction == ControlAction.LEFT
        val movingRight = controlAction == ControlAction.RIGHT
        val isChangingToLeft = movingLeft && player.direction == Direction.RIGHT
        val isChangingToRight = movingRight && player.direction == Direction.LEFT
        val isChangingDirection = isChangingToLeft || isChangingToRight
        if (isChangingDirection) {
            logger.info { "direction changed player vx was: ${player.vx} and is now 0" }
            player.vx = 0.0
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
