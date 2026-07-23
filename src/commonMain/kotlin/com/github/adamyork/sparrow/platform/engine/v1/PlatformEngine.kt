package com.github.adamyork.sparrow.platform.engine.v1

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.AudioQueue
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.*
import com.github.adamyork.sparrow.platform.common.data.enemy.BlockerEnemy
import com.github.adamyork.sparrow.platform.common.data.enemy.Enemy
import com.github.adamyork.sparrow.platform.common.data.enemy.EnemyType
import com.github.adamyork.sparrow.platform.common.data.enemy.RunnerEnemy
import com.github.adamyork.sparrow.platform.common.data.item.CollectibleItem
import com.github.adamyork.sparrow.platform.common.data.item.DefaultItem
import com.github.adamyork.sparrow.platform.common.data.item.Item
import com.github.adamyork.sparrow.platform.common.data.item.ItemType
import com.github.adamyork.sparrow.platform.common.data.map.GameMap
import com.github.adamyork.sparrow.platform.common.data.map.GameMapState
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.common.data.player.PlayerJumpingState
import com.github.adamyork.sparrow.platform.common.data.player.PlayerMovingState
import com.github.adamyork.sparrow.platform.engine.Collision
import com.github.adamyork.sparrow.platform.engine.Engine
import com.github.adamyork.sparrow.platform.engine.Particles
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.platform.engine.data.DrawResult
import com.github.adamyork.sparrow.platform.engine.data.PlatformImage
import com.github.adamyork.sparrow.platform.service.AssetService
import com.github.adamyork.sparrow.platform.service.RuntimeService
import com.github.adamyork.sparrow.platform.service.ScoreService
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import com.github.adamyork.sparrow.platform.service.data.ImageAsset
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
abstract class PlatformEngine @AppScope @Inject constructor(
    val physics: Physics,
    val collision: Collision,
    val particles: Particles,
    val audioQueue: AudioQueue,
    val scoreService: ScoreService,
    val assetService: AssetService,
    val runtimeService: RuntimeService,
    val platformInterop: PlatformInterop
) : Engine {

    private val logger = KotlinLogging.logger {}

    protected open var mapItem: Item = DefaultItem()
    abstract var mapItemImage: PlatformImage
    abstract var playerImage: PlatformImage

    protected open val itemImageCache: HashMap<String, PlatformImage> = hashMapOf()
    protected open val enemyImageCache: HashMap<String, PlatformImage> = hashMapOf()
    protected open val flippedFrameCache: HashMap<String, PlatformImage> = hashMapOf()

    abstract var foregroundSurface: Any?

    abstract val mapElementPaint: Any
    abstract val particlePaint: Any
    abstract val mapItemReturnPaint: Any
    abstract val playerPaintNormal: Any
    abstract val playerPaintTinted: Any

    abstract fun getOrCreateForegroundSurface(viewPort: ViewPort): Any

    override suspend fun initialize(gameMap: GameMap, collisionImageAndBytes: ImageAndBytes, player: Player, font: Any) {
        throw Exception("must implemented")
    }

    override fun getCollisionBoundaries(player: Player): CollisionBoundaries =
        collision.getCollisionBoundaries(player)

    override fun managePlayer(player: Player, collisionBoundaries: CollisionBoundaries) {
        physics.applyPlayerPhysics(player, collisionBoundaries, collision)
        val (metadata, metadataState) = player.getNextFrameMetadataWithState()
        player.frameMetadata = metadata
        player.colliding = metadataState.colliding
    }

    override fun manageViewport(player: Player, viewPort: ViewPort) {
        val previousX = viewPort.x
        val previousY = viewPort.y
        val nextX = when (player.direction) {
            Direction.RIGHT -> {
                val adjustedX = player.x + player.width
                val viewPortRightBoundary = viewPort.x + viewPort.width
                if (adjustedX > viewPortRightBoundary) {
                    (viewPort.x + (adjustedX - viewPortRightBoundary))
                        .coerceAtMost(collision.collisionImage.imageBitmap.width - viewPort.width)
                } else viewPort.x
            }

            Direction.LEFT -> {
                if (player.x < viewPort.x) {
                    (viewPort.x - (viewPort.x - player.x))
                        .coerceAtLeast(0)
                } else viewPort.x
            }
        }
        val nextY = when {
            player.y < viewPort.y -> (viewPort.y - (viewPort.y - player.y)).coerceAtLeast(0)
            (player.y + player.height) > (viewPort.y + viewPort.height) -> (viewPort.y + (player.y - viewPort.y)).coerceAtMost(
                collision.collisionImage.imageBitmap.height - viewPort.height
            )

            else -> viewPort.y
        }
        viewPort.x = nextX
        viewPort.y = nextY
        viewPort.lastX = viewPort.x
        viewPort.lastY = viewPort.y
        if (previousX != nextX || previousY != nextY) {
            logger.info { "Viewport moved: ($previousX, $previousY) -> ($nextX, $nextY)" }
        }
    }

    override fun manageMap(player: Player, gameMap: GameMap, viewPort: ViewPort) {
        manageMapItems(gameMap)
        manageMapEnemies(gameMap, player)
        physics.applyCollisionParticlePhysics(gameMap.particles, viewPort)
        physics.applyMapItemReturnParticlePhysics(gameMap.particles, viewPort)
        if (player.moving == PlayerMovingState.MOVING && player.jumping == PlayerJumpingState.GROUNDED) {
            particles.applyDustParticles(player, gameMap.particles)
        }
        physics.applyDustParticlePhysics(gameMap.particles)
        physics.applyProjectileParticlePhysics(gameMap.particles, viewPort)
        val allCollectiblesFound = scoreService.allFound()
        gameMap.state = when (gameMap.state) {
            GameMapState.COLLECTING if allCollectiblesFound -> GameMapState.COMPLETING
            GameMapState.COMPLETING if !allCollectiblesFound -> GameMapState.COLLECTING
            else -> gameMap.state
        }
    }

    override fun manageEnemyAndItemCollision(player: Player, map: GameMap, viewPort: ViewPort) {
        collision.applyAllItemCollision(player, map, audioQueue)
        collision.applyEnemyAndProximityCollision(player, map, viewPort, audioQueue, particles)
        collision.applyProjectileCollision(player, map, viewPort, audioQueue, particles)
        if (player.colliding == GameElementCollisionState.COLLIDING) {
            adjustMapAfterItemCollision(map)
        }
    }

    protected fun manageMapItems(gameMap: GameMap) {
        for ((index, item) in gameMap.items.withIndex()) {
            val (metadata, metadataState) = (item as GameElement).getNextFrameMetadataWithState()
            var nextState = metadataState.state
            if (item.type == ItemType.FINISH) {
                if (gameMap.state == GameMapState.COMPLETING && item.state == ElementState.INACTIVE) {
                    nextState = ElementState.ACTIVE
                } else if (gameMap.state == GameMapState.COLLECTING && item.state != ElementState.INACTIVE) {
                    nextState = ElementState.INACTIVE
                }
            }
            item.state = nextState
            item.frameMetadata = metadata
            gameMap.items[index] = item
        }
    }

    protected fun adjustMapAfterItemCollision(gameMap: GameMap) {
        val index = gameMap.items.indexOfFirst { item ->
            item.type == ItemType.COLLECTABLE && item.state == ElementState.INACTIVE
        }
        if (index != -1) {
            val item = gameMap.items[index]
            if (item is CollectibleItem) {
                item.state = ElementState.ACTIVE
                gameMap.items[index] = item
                if (gameMap.state == GameMapState.COMPLETING) {
                    gameMap.state = GameMapState.COLLECTING
                }
            }
        }
    }

    protected fun manageMapEnemies(gameMap: GameMap, player: Player) {
        val deltaTimeCoefficient = runtimeService.getDeltaTimeCoefficient()
        for ((index, enemy) in gameMap.enemies.withIndex()) {
            val nextState = enemy.getNextEnemyState(player)
            if (nextState != ElementState.INACTIVE) {
                val nextPosition = when (enemy) {
                    is BlockerEnemy -> enemy.getNextPosition(deltaTimeCoefficient)
                    is RunnerEnemy -> enemy.getNextPosition(deltaTimeCoefficient)
                    else -> enemy.getNextPosition()
                }
                val (metadata, metadataState) = (enemy as GameElement).getNextFrameMetadataWithState()
                enemy.x = nextPosition.x
                enemy.y = nextPosition.y
                enemy.state = nextState
                enemy.frameMetadata = metadata
                enemy.enemyPosition = nextPosition
                enemy.colliding = metadataState.colliding
                enemy.interacting = metadataState.interacting
            } else if (enemy.type == EnemyType.RUNNER) {
                enemy.state = nextState
            }
            gameMap.enemies[index] = enemy
        }
    }

    override fun draw(map: GameMap, viewPort: ViewPort, player: Player, timestamp: Double): DrawResult {
        throw Exception("must be implemented")
    }

    override fun createDefaultPlayer(playerAsset: ImageAsset): Player {
        return Player(
            assetService.appProperties.player.x,
            assetService.appProperties.player.y,
            playerAsset.width,
            playerAsset.height,
            ElementState.ACTIVE,
            FrameMetadata(1, Cell(1, 1, playerAsset.width, playerAsset.height)),
            playerAsset.imageAndBytes,
            0.0,
            0.0,
            PlayerJumpingState.GROUNDED,
            PlayerMovingState.STATIONARY,
            Direction.RIGHT,
            GameElementCollisionState.FREE,
            platformInterop,
            0,
            assetService.appProperties.engine.fps.animation.toDouble()
        )
    }

    override fun startInput(controlAction: ControlAction, player: Player) {
        when (controlAction) {
            ControlAction.LEFT, ControlAction.RIGHT -> {
                val previousDirection = player.direction
                val direction = if (controlAction == ControlAction.LEFT) Direction.LEFT else Direction.RIGHT
                player.moving = PlayerMovingState.MOVING
                player.direction = direction
                if (previousDirection != player.direction) {
                    logger.debug { "Player direction changed: $previousDirection -> ${player.direction}" }
                }
                physics.changeXVelocityIfDirectionChanged(controlAction, player)
            }

            ControlAction.JUMP -> {
                if (player.jumping == PlayerJumpingState.GROUNDED) {
                    audioQueue.queue.add(Sounds.JUMP)
                    player.jumping = PlayerJumpingState.INITIAL
                }
            }
        }
    }

    override fun stopInput(controlAction: ControlAction, player: Player) {
        val movingLeft = controlAction == ControlAction.LEFT
        val movingRight = controlAction == ControlAction.RIGHT
        val isStoppingLeft = movingLeft && player.direction == Direction.LEFT
        val isStoppingRight = movingRight && player.direction == Direction.RIGHT
        if (isStoppingLeft || isStoppingRight) {
            player.moving = PlayerMovingState.STATIONARY
        }
    }

    protected fun itemCacheKey(item: Item): String = "${item.type.name}:${item.id}"

    protected fun enemyCacheKey(enemy: Enemy): String = "${enemy.type.name}:${enemy.id}"

    protected fun hasVisibleActiveElements(elements: ArrayList<out GameElement>, viewPort: ViewPort): Boolean {
        return elements.any { it.state != ElementState.INACTIVE && it.cullingCheck(viewPort) }
    }
}
