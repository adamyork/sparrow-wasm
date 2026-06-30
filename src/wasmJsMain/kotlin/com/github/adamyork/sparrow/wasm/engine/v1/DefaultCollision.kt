package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.data.*
import com.github.adamyork.sparrow.wasm.common.data.enemy.*
import com.github.adamyork.sparrow.wasm.common.data.item.CollectibleItem
import com.github.adamyork.sparrow.wasm.common.data.item.FinishItem
import com.github.adamyork.sparrow.wasm.common.data.item.ItemType
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.common.v1.DefaultAudioQueue
import com.github.adamyork.sparrow.wasm.engine.Collision
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.engine.Physics
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.engine.data.ParticleType
import com.github.adamyork.sparrow.wasm.service.ScoreService
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.Point
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@AppScope
@Inject
/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class DefaultCollision(
    private val physics: Physics,
    private val scoreService: ScoreService
) : Collision {

    companion object {
        const val COLLISION_COLOR_VALUE: Int = -16711906
    }

    private val logger = KotlinLogging.logger {}

    override lateinit var collisionImage: ImageAndBytes
    private lateinit var collisionMask: BooleanArray
    private var bitmapWidth: Int = 0
    private var bitmapHeight: Int = 0

    override fun cacheCollisionPixels() {
        val image = Image.makeFromEncoded(collisionImage.bytes)
        val bitmap = Bitmap.makeFromImage(image)
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height

        val pixelMap = bitmap.peekPixels() ?: throw IllegalStateException("Failed to peek pixels")
        collisionMask = BooleanArray(bitmapWidth * bitmapHeight)
        for (y in 0 until bitmapHeight) {
            val rowOffset = y * bitmapWidth
            for (x in 0 until bitmapWidth) {
                collisionMask[rowOffset + x] = (pixelMap.getColor(x, y) == COLLISION_COLOR_VALUE)
            }
        }
    }

    override fun getCollisionBoundaries(player: Player): CollisionBoundaries {
        return CollisionBoundaries(
            findEdgeIterative(player.x, player, Direction.LEFT),
            findEdgeIterative(player.x, player, Direction.RIGHT),
            findCeilingIterative(player.y, player),
            findFloorIterative(player.y, player)
        )
    }

    override fun recomputeXBoundaries(
        player: Player,
        previousBoundaries: CollisionBoundaries
    ): CollisionBoundaries {
        return CollisionBoundaries(
            findEdgeIterative(player.x, player, Direction.LEFT),
            findEdgeIterative(player.x, player, Direction.RIGHT),
            previousBoundaries.top,
            previousBoundaries.bottom
        )
    }

    private fun findFloorIterative(startY: Int, player: Player): Int {
        for (y in startY until bitmapHeight) {
            if (testMaskCollision(player.x, y, player.width, 1)) return y - player.height
        }
        return bitmapHeight - player.height
    }

    private fun findCeilingIterative(startY: Int, player: Player): Int {
        for (y in startY downTo 0) {
            if (testMaskCollision(player.x, y, player.width, 1)) return y + 1
        }
        return 0
    }

    private fun findEdgeIterative(startX: Int, player: Player, direction: Direction): Int {
        val range = if (direction == Direction.RIGHT) startX until bitmapWidth else startX downTo 0
        for (x in range) {
            if (testMaskCollision(x, player.y, 1, player.height)) {
                return if (direction == Direction.RIGHT) x - player.width else x
            }
        }
        return if (direction == Direction.RIGHT) bitmapWidth - player.width else 0
    }

    private fun testMaskCollision(x: Int, y: Int, width: Int, height: Int): Boolean {
        if (x < 0 || y < 0 || x + width > bitmapWidth || y + height > bitmapHeight) return true
        for (yi in y until (y + height)) {
            val rowOffset = yi * bitmapWidth
            for (xi in x until (x + width)) {
                if (collisionMask[rowOffset + xi]) return true
            }
        }
        return false
    }

    override fun checkForItemCollision(player: Player, gameMap: GameMap, audioQueue: DefaultAudioQueue): GameMap {
        var gameState = gameMap.state
        val playerRect = player.toRect()
        val managedMapItems = gameMap.items.map { item ->
            var nextItemState = item.state
            var nextFrameMetaData = item.frameMetadata
            if (playerRect.overlaps(item.toRect()) && nextItemState == GameElementState.ACTIVE) {
                if (item.type == ItemType.FINISH) {
                    gameState = GameMapState.COMPLETED
                    nextItemState = GameElementState.INACTIVE
                } else {
                    nextItemState = GameElementState.DEACTIVATING
                    audioQueue.queue.add(Sounds.ITEM_COLLECT)
                    nextFrameMetaData = item.getFirstDeactivatingFrame()
                }
            }
            if (item is FinishItem) item.copy(state = nextItemState, frameMetadata = nextFrameMetaData)
            else (item as CollectibleItem).copy(state = nextItemState, frameMetadata = nextFrameMetaData)
        }.toCollection(ArrayList())
        return gameMap.copy(state = gameState, items = managedMapItems)
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun checkForEnemyCollisionAndProximity(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: DefaultAudioQueue,
        particles: Particles
    ): Pair<Player, GameMap> {
        val managedMapParticles = gameMap.particles.toCollection(ArrayList())
        var playerIsColliding = false
        var closestEnemyRect: Rect? = null
        var minDistance = Float.MAX_VALUE
        val playerRect = player.toRect()
        val isCollisionAnimating = managedMapParticles.any { it.type == ParticleType.COLLISION }
        val managedMapEnemies = gameMap.enemies.map { enemy ->
            val element = enemy as GameElement
            if (element.state == GameElementState.INACTIVE) return@map enemy
            val enemyRect = enemy.toRect()
            val isColliding = playerRect.overlaps(enemyRect)
            var isInteracting = false
            if (isColliding) {
                val dist = distanceTo(
                    Point(player.x.toFloat(), player.y.toFloat()),
                    Point(enemy.x.toFloat(), enemy.y.toFloat())
                )
                if (dist < minDistance) {
                    minDistance = dist.toFloat()
                    closestEnemyRect = enemyRect
                }
                if (isCollisionAnimating) {
                    val firstCollisionParticle = managedMapParticles.firstOrNull { it.type == ParticleType.COLLISION }
                    val collisionId = firstCollisionParticle?.collisionId
                    val totalCollsionPixels = managedMapParticles.filter { it.type == ParticleType.COLLISION }.size
                    logger.info { "collision is animating still $collisionId" }
                    logger.info { "totalCollsionPixels $totalCollsionPixels" }
                    logger.info { "player x ${player.x} and player.y ${player.y}" }
                    logger.info { "viewport x ${viewPort.x} and viewport.y ${viewPort.y}" }
                    logger.info { "firstCollisionParticle x ${firstCollisionParticle?.x} and firstCollisionParticle.y ${firstCollisionParticle?.y}" }
                }
                if (!isCollisionAnimating && enemy.colliding != GameElementCollisionState.COLLIDING) {
                    logger.info { "collision adding particles" }
                    val collisionId = Uuid.random().toString()
                    audioQueue.queue.add(Sounds.PLAYER_COLLISION)
                    managedMapParticles.addAll(particles.createCollisionParticles(enemy.x, enemy.y, collisionId))
                    if (scoreService.getTotal() != scoreService.getRemaining()) {
                        managedMapParticles.add(particles.createMapItemReturnParticle(player))
                    }
                }
                playerIsColliding = true
            }
            if (!isColliding && enemy.type == EnemyType.SHOOTER &&
                enemy.interacting != EnemyInteractionState.INTERACTING &&
                distanceTo(
                    Point(player.x.toFloat(), player.y.toFloat()),
                    Point(enemy.x.toFloat(), enemy.y.toFloat())
                ) <= ShooterEnemy.PLAYER_PROXIMITY_THRESHOLD
            ) {
                val (newParticles, added) = particles.createProjectileParticle(player, enemy, managedMapParticles)
                if (added) {
                    isInteracting = true
                    audioQueue.queue.add(Sounds.ENEMY_SHOOT)
                }
                managedMapParticles.addAll(newParticles)
            }
            val (metadata, _) = element.getNextFrameMetadataWithState()
            val isAnimationFinished = metadata.frame >= 7
            val nextInteracting = when {
                isColliding -> EnemyInteractionState.ISOLATED
                isInteracting -> EnemyInteractionState.INTERACTING
                enemy.interacting == EnemyInteractionState.INTERACTING && isAnimationFinished -> EnemyInteractionState.ISOLATED
                else -> enemy.interacting
            }
            val nextColliding = if (isColliding) GameElementCollisionState.COLLIDING else GameElementCollisionState.FREE
            when (enemy) {
                is ShooterEnemy -> enemy.copy(
                    frameMetadata = metadata,
                    colliding = nextColliding,
                    interacting = nextInteracting
                )

                is RunnerEnemy -> enemy.copy(
                    frameMetadata = metadata,
                    colliding = nextColliding,
                    interacting = nextInteracting
                )

                else -> (enemy as BlockerEnemy).copy(
                    frameMetadata = metadata,
                    colliding = nextColliding,
                    interacting = nextInteracting
                )
            }
        }.toCollection(ArrayList())

        val nextPlayer = if (playerIsColliding && closestEnemyRect != null) {
            physics.applyPlayerCollisionPhysics(player, closestEnemyRect, viewPort)
        } else {
            player
        }

        return Pair(nextPlayer, gameMap.copy(enemies = managedMapEnemies, particles = managedMapParticles))
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun checkForProjectileCollision(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: DefaultAudioQueue,
        particles: Particles
    ): Pair<Player, GameMap> {
        var playerIsColliding = false
        var targetRect: Rect? = null
        val playerRect = player.toRect()
        val updatedParticles = gameMap.particles.filter { particle ->
            if (particle.type == ParticleType.PROJECTILE && playerRect.overlaps(particle.toRect())) {
                targetRect = particle.toRect()
                audioQueue.queue.add(Sounds.PLAYER_COLLISION)
                playerIsColliding = true
                false
            } else {
                true
            }
        }.toCollection(ArrayList())
        val isCollisionAnimating = updatedParticles.any { it.type == ParticleType.COLLISION }
        if (playerIsColliding && !isCollisionAnimating) {
            val collisionId = Uuid.random().toString()
            updatedParticles.addAll(particles.createCollisionParticles(player.x, player.y, collisionId))

            if (scoreService.getTotal() != scoreService.getRemaining()) {
                updatedParticles.add(particles.createMapItemReturnParticle(player))
            }
        }
        val adjustedTargetRect = targetRect?.inflate(ShooterEnemy.PLAYER_PROXIMITY_THRESHOLD.toFloat())
        val nextPlayer = if (playerIsColliding && adjustedTargetRect != null) {
            physics.applyPlayerCollisionPhysics(player, adjustedTargetRect, viewPort)
        } else {
            player
        }
        return Pair(nextPlayer, gameMap.copy(particles = updatedParticles))
    }

    fun GameElement.toRect() = Rect(
        x.toFloat(),
        y.toFloat(),
        (x + width).toFloat(),
        (y + height).toFloat()
    )

    private fun distanceTo(first: Point, second: Point): Int {
        return sqrt((second.x - first.x).pow(2) + (second.y - first.y).pow(2)).toInt()
    }
}