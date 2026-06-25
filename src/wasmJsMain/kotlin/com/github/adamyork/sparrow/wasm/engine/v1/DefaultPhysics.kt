package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.DefaultStatusProvider
import com.github.adamyork.sparrow.wasm.data.Direction
import com.github.adamyork.sparrow.wasm.data.GameElementCollisionState
import com.github.adamyork.sparrow.wasm.data.ViewPort
import com.github.adamyork.sparrow.wasm.data.player.Player
import com.github.adamyork.sparrow.wasm.data.player.PlayerJumpingState
import com.github.adamyork.sparrow.wasm.data.player.PlayerMovingState
import com.github.adamyork.sparrow.wasm.engine.Collision
import com.github.adamyork.sparrow.wasm.engine.Physics
import com.github.adamyork.sparrow.wasm.engine.data.*
import com.github.adamyork.sparrow.wasm.service.PhysicsSettingsService
import me.tatarka.inject.annotations.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
        val vx = getXVelocity(player.vx, player.moving)
        val vy = getYVelocity(player.vy, player.jumping)
        val yResult = movePlayerY(
            player.y,
            vy,
            player.jumping,
            collisionBoundaries
        )
        var adjustedCollisionBoundaries = collisionBoundaries
        if (player.y != yResult.y) {
            val nextPlayer = player.copy(y = yResult.y, vy = yResult.vy, jumping = yResult.jumping)
            adjustedCollisionBoundaries =
                collision.recomputeXBoundaries(nextPlayer, collisionBoundaries)
        }
        val xResult = movePlayerX(
            player.x,
            vx,
            player.moving,
            player.direction,
            adjustedCollisionBoundaries
        )
        return player.copy(
            x = xResult.x,
            vx = xResult.vx,
            moving = xResult.moving,
            y = yResult.y,
            vy = yResult.vy,
            jumping = yResult.jumping
        )
    }

    override fun applyPlayerCollisionPhysics(
        player: Player,
        rect: Rect?,
        viewPort: ViewPort,
    ): Player {
        val collisionRect = rect ?: Rect(0F, 0F, 0F, 0F)
        var nextX = player.x
        if (player.direction == Direction.LEFT) {
            nextX += player.width
            if (nextX >= viewPort.width - player.width) {
                nextX = viewPort.width - player.width - 1
            }
        } else {
            nextX -= player.width
            if (nextX < 0) {
                nextX = 0
            }
        }
        val playerRect = Rect(nextX.toFloat(), player.y.toFloat(), player.width.toFloat(), player.height.toFloat())
        if (playerRect.overlaps(collisionRect)) {
            //LOGGER.info("adjusted player for collision but still colliding ! ${player.direction}")
            if (player.direction == Direction.LEFT) {
                nextX -= collisionRect.width.toInt() * 2
            } else {
                nextX += collisionRect.width.toInt() * 2
            }
        }
        return player.copy(x = nextX, vx = 0.0, colliding = GameElementCollisionState.COLLIDING)
    }

    private fun getXVelocity(playerVx: Double, playerMoving: PlayerMovingState): Double {
        var nextVx: Double = playerVx
        if (playerMoving == PlayerMovingState.MOVING) {
            if (nextVx == 0.0) {
                nextVx = physicsSettingsService.xMovementDistance
            }
            nextVx =
                physicsSettingsService.xMovementDistance * (nextVx * physicsSettingsService.xAccelerationRate)
            nextVx *= physicsSettingsService.friction
            if (nextVx > physicsSettingsService.maxXVelocity) {
                nextVx = physicsSettingsService.maxXVelocity
            }
        } else {
            if (nextVx > 0.0) {
                nextVx -= physicsSettingsService.xDeaccelerationRate
                if (nextVx < 0) {
                    nextVx = 0.0
                }
            }
        }
        return nextVx
    }

    private fun getYVelocity(playerVy: Double, playerJumping: PlayerJumpingState): Double {
        var nextVy: Double = playerVy
        if (playerJumping == PlayerJumpingState.GROUNDED || playerJumping == PlayerJumpingState.HEIGHT_REACHED || playerJumping == PlayerJumpingState.FALLING) {
            nextVy = 0.0
        } else {
            if (playerJumping == PlayerJumpingState.INITIAL) {
                //LOGGER.info("starting a jump INITIAL")
                nextVy += physicsSettingsService.jumpDistance / 2
            } else if (playerJumping == PlayerJumpingState.RISING) {
                //LOGGER.info("in a jump RISING")
                nextVy += (physicsSettingsService.yVelocityCoefficient * nextVy)
                if (nextVy > physicsSettingsService.maxYVelocity) {
                    nextVy = physicsSettingsService.maxYVelocity
                }
            }
        }
        return nextVy
    }

    private fun movePlayerX(
        playerX: Int,
        playerVx: Double,
        playerMoving: PlayerMovingState,
        playerDirection: Direction,
        collisionBoundaries: CollisionBoundaries
    ): PhysicsXResult {
        var targetX = playerX
        val deltaTime = statusProvider.getDeltaTimeCoefficient()
        if (playerMoving == PlayerMovingState.MOVING || playerVx != 0.0) {
            if (playerDirection == Direction.LEFT) {
                targetX -= (playerVx * deltaTime).roundToInt()
                if (targetX <= collisionBoundaries.left) {
                    //LOGGER.info("targetX $targetX less or equal to the left boundary ${collisionBoundaries.left}")
                    //LOGGER.info("(left) playerVx $playerVx and $deltaTime")
                    targetX = collisionBoundaries.left + 1
                }
            } else {
                targetX += (playerVx * deltaTime).roundToInt()
                if (targetX >= collisionBoundaries.right) {
                    //LOGGER.info("targetX $targetX greater or equal to the right boundary ${collisionBoundaries.right}")
                    //LOGGER.info("(right) playerVx $playerVx and $deltaTime")
                    targetX = (collisionBoundaries.right - 1).coerceAtLeast(0)
                }
            }
        }
        return PhysicsXResult(targetX, playerVx, playerMoving)
    }

    private fun movePlayerY(
        playerY: Int,
        vy: Double,
        playerJumping: PlayerJumpingState,
        collisionBoundaries: CollisionBoundaries
    ): PhysicsYResult {
        var destinationY = playerY + physicsSettingsService.gravity.roundToInt()
        var nextPlayerJumping = playerJumping
        var nextPlayerVy = vy
        val deltaTime = statusProvider.getDeltaTimeCoefficient()
        destinationY -= (vy * deltaTime).roundToInt()
        if (nextPlayerJumping == PlayerJumpingState.HEIGHT_REACHED) {
            //LOGGER.info("player jump is FALLING")
            nextPlayerJumping = PlayerJumpingState.FALLING
        }
        if (nextPlayerJumping == PlayerJumpingState.INITIAL || nextPlayerJumping == PlayerJumpingState.RISING) {
            if (nextPlayerJumping == PlayerJumpingState.INITIAL) {
                nextPlayerJumping = PlayerJumpingState.RISING
            }
            val jumpBoundary = collisionBoundaries.bottom - physicsSettingsService.jumpDistance
            if (destinationY <= jumpBoundary) {
                //LOGGER.info("jump height reached HEIGHT_REACHED")
                nextPlayerJumping = PlayerJumpingState.HEIGHT_REACHED
                nextPlayerVy = 0.0
            }
        }
        if (destinationY > collisionBoundaries.bottom) {
            destinationY = collisionBoundaries.bottom
            if (nextPlayerJumping == PlayerJumpingState.FALLING) {
                nextPlayerJumping = PlayerJumpingState.GROUNDED
                //LOGGER.info("jump complete GROUNDED")
            }
        } else if (destinationY < collisionBoundaries.top) {
            destinationY = collisionBoundaries.top + 1
            if (nextPlayerJumping == PlayerJumpingState.RISING) {
                //LOGGER.info("jump height reached because of top of viewport HEIGHT_REACHED")
                nextPlayerJumping = PlayerJumpingState.HEIGHT_REACHED
                nextPlayerVy = 0.0
            }
        }
        return PhysicsYResult(
            destinationY,
            nextPlayerVy,
            nextPlayerJumping
        )
    }

    override fun applyCollisionParticlePhysics(mapParticles: ArrayList<Particle>): ArrayList<Particle> {
        return mapParticles
            .map { particle ->
                if (particle.type == ParticleType.COLLISION) {
                    val nextFrame = particle.frame + 1
                    var nextRadius = particle.radius
                    var position = Pair(particle.x.toFloat(), particle.y.toFloat())
                    if (particle.radius < DefaultParticles.MAX_SQUARE_RADIAL_RADIUS) {
                        nextRadius = particle.radius + 10
                        position =
                            getCollisionParticlePosition(
                                nextRadius.toFloat(),
                                particle.id.toFloat(),
                                particle.originX,
                                particle.originY
                            )
                    } else {
                        if (particle.frame <= particle.lifetime) {
                            position = Pair(
                                particle.x.toFloat(),
                                particle.y.toFloat() + physicsSettingsService.gravity.toFloat()
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
            }.filter { particle -> particle.frame <= particle.lifetime }
            .toCollection(ArrayList())
    }

    override fun applyDustParticlePhysics(mapParticles: ArrayList<Particle>): ArrayList<Particle> {
        return mapParticles
            .map { particle ->
                if (particle.type == ParticleType.DUST) {
                    val nextFrame = particle.frame + 1
                    val nextWidth = (particle.width + 1).coerceAtMost(40)
                    val nextHeight = (particle.height + 1).coerceAtMost(40)
                    val nextRadius = (particle.radius - 15).coerceAtLeast(0)
                    particle.copy(width = nextWidth, height = nextHeight, frame = nextFrame, radius = nextRadius)
                } else {
                    particle
                }
            }.filter { particle -> particle.frame <= particle.lifetime }
            .toCollection(ArrayList())
    }

    override fun applyProjectileParticlePhysics(mapParticles: ArrayList<Particle>): ArrayList<Particle> {
        return mapParticles
            .map { particle ->
                if (particle.type == ParticleType.PROJECTILE) {
                    val nextFrame = particle.frame + 1
                    val nextX: Int = if (particle.originX == particle.x) {
                        particle.x
                    } else if (particle.originX < particle.x) {
                        particle.x - particle.xJitter
                    } else {
                        particle.x + particle.xJitter
                    }
                    val nextY: Int = if (particle.originY == particle.y) {
                        particle.y
                    } else if (particle.originY < particle.y) {
                        particle.y - particle.yJitter
                    } else {
                        particle.y + particle.yJitter
                    }
                    particle.copy(x = nextX, y = nextY, frame = nextFrame)
                } else {
                    particle
                }
            }.filter { particle -> particle.frame <= particle.lifetime }
            .toCollection(ArrayList())
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
