package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.AudioQueue
import com.github.adamyork.sparrow.wasm.common.data.*
import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyInteractionState
import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyType
import com.github.adamyork.sparrow.wasm.common.data.enemy.ShooterEnemy
import com.github.adamyork.sparrow.wasm.common.data.item.FinishItem
import com.github.adamyork.sparrow.wasm.common.data.item.ItemType
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.Collision
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.engine.Physics
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.engine.data.Particle
import com.github.adamyork.sparrow.wasm.engine.data.ParticleType
import com.github.adamyork.sparrow.wasm.service.ScoreService
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.math.absoluteValue

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultCollision(
    private val physics: Physics,
    private val scoreService: ScoreService
) : Collision {

    companion object {
        const val COLLISION_COLOR_VALUE: Int = -16711906
        private const val SHOOTER_PROXIMITY_THRESHOLD_SQUARED: Int =
            ShooterEnemy.PLAYER_PROXIMITY_THRESHOLD * ShooterEnemy.PLAYER_PROXIMITY_THRESHOLD
    }

    private val logger = KotlinLogging.logger {}

    override lateinit var collisionImage: ImageAndBytes
    private lateinit var collisionMask: BooleanArray
    private var bitmapWidth: Int = 0
    private var bitmapHeight: Int = 0
    private var lastPlayerX: Int = -1
    private var lastPlayerY: Int = -1
    private var cachedBoundaries: CollisionBoundaries? = null

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
        if (player.x == lastPlayerX && player.y == lastPlayerY && cachedBoundaries != null) {
            return cachedBoundaries!!
        }
        lastPlayerX = player.x
        lastPlayerY = player.y
        cachedBoundaries = CollisionBoundaries(
            findEdgeIterative(player.x, player, Direction.LEFT),
            findEdgeIterative(player.x, player, Direction.RIGHT),
            findCeilingIterative(player.y, player),
            findFloorIterative(player.y, player)
        )
        return cachedBoundaries!!
    }


    override fun updateCollisionXBoundaries(
        player: Player,
        collisionBoundaries: CollisionBoundaries
    ) {
        collisionBoundaries.left = findEdgeIterative(player.x, player, Direction.LEFT)
        collisionBoundaries.right = findEdgeIterative(player.x, player, Direction.RIGHT)
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
        val movementDelta = player.vx.absoluteValue.toInt()
        val maxLookAhead = movementDelta + 2
        val maxPossibleX = bitmapWidth - player.width
        val range = if (direction == Direction.RIGHT) {
            startX until (startX + maxLookAhead).coerceAtMost(maxPossibleX)
        } else {
            startX downTo (startX - maxLookAhead).coerceAtLeast(0)
        }
        for (x in range) {
            val checkX = if (direction == Direction.RIGHT) x + player.width - 1 else x
            if (testMaskCollision(checkX.coerceIn(0, bitmapWidth - 1), player.y, 1, player.height)) {
                return if (direction == Direction.RIGHT) x - 1 else x + 1
            }
        }
        val endPosition = if (direction == Direction.RIGHT) startX + maxLookAhead else startX - maxLookAhead
        return endPosition.coerceIn(0, maxPossibleX)
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

    override fun applyAllItemCollision(player: Player, gameMap: GameMap, audioQueue: AudioQueue) {
        val items = gameMap.items
        val playerRect = player.toRect()
        var newGameState = gameMap.state
        for (i in items.indices) {
            val item = items[i]
            if (item.state == GameElementState.ACTIVE && playerRect.overlaps(item.toRect())) {
                if (item.type == ItemType.FINISH) {
                    newGameState = GameMapState.COMPLETED
                    val finishItem = item as FinishItem
                    finishItem.state = GameElementState.INACTIVE
                    items[i] = finishItem
                } else {
                    audioQueue.queue.add(Sounds.ITEM_COLLECT)
                    item.state = GameElementState.DEACTIVATING
                    item.frameMetadata = item.getFirstDeactivatingFrame()
                    items[i] = item
                }
            }
        }
        if (newGameState != gameMap.state) {
            gameMap.state = newGameState
        }
    }

    override fun applyEnemyAndProximityCollision(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: AudioQueue,
        particles: Particles
    ) {
        val managedMapParticles = gameMap.particles
        val managedMapEnemies = gameMap.enemies
        val canTakeCollisionDamage = player.immunityTicks <= 0
        var playerIsColliding = false
        var closestEnemyRect: Rect? = null
        var minDistanceSquared = Int.MAX_VALUE
        val playerRect = player.toRect()
        val isCollisionAnimating = Particle.hasActiveVisibleCollisionParticles(managedMapParticles, viewPort)
        for (i in managedMapEnemies.indices) {
            val enemy = managedMapEnemies[i]
            val element = enemy as GameElement
            if (element.state == GameElementState.INACTIVE) continue
            val enemyRect = enemy.toRect()
            val isColliding = playerRect.overlaps(enemyRect)
            var isInteracting = false
            if (isColliding && canTakeCollisionDamage) {
                val distanceSquared = distanceSquared(player.x, player.y, enemy.x, enemy.y)
                if (distanceSquared < minDistanceSquared) {
                    minDistanceSquared = distanceSquared
                    closestEnemyRect = enemyRect
                }
                if (!isCollisionAnimating) {
                    logger.info { "collision adding particles" }
                    audioQueue.queue.add(Sounds.PLAYER_COLLISION)
                    applyCollisionAndMapItemReturnParticles(particles, player, managedMapParticles, gameMap)
                }
                playerIsColliding = true
            }
            if (!isColliding && enemy.type == EnemyType.SHOOTER &&
                enemy.interacting != EnemyInteractionState.INTERACTING &&
                distanceSquared(player.x, player.y, enemy.x, enemy.y) <= SHOOTER_PROXIMITY_THRESHOLD_SQUARED
            ) {
                val added = particles.applyProjectileParticle(player, enemy, managedMapParticles)
                if (added) {
                    isInteracting = true
                    audioQueue.queue.add(Sounds.ENEMY_SHOOT)
                }
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
            enemy.frameMetadata = metadata
            enemy.colliding = nextColliding
            enemy.interacting = nextInteracting
            managedMapEnemies[i] = enemy
        }
        if (playerIsColliding && closestEnemyRect != null) {
            physics.applyPlayerCollisionPhysics(player, closestEnemyRect, viewPort)
        }
    }

    override fun applyProjectileCollision(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: AudioQueue,
        particles: Particles
    ) {
        val canTakeCollisionDamage = player.immunityTicks <= 0
        var playerIsColliding = false
        var targetRect: Rect? = null
        val playerRect = player.toRect()
        val particleList = gameMap.particles
        for (i in particleList.indices.reversed()) {
            val particle = particleList[i]
            if (canTakeCollisionDamage &&
                particle.type == ParticleType.PROJECTILE &&
                playerRect.overlaps(particle.toRect())
            ) {
                targetRect = particle.toRect()
                audioQueue.queue.add(Sounds.PLAYER_COLLISION)
                playerIsColliding = true
                particleList.removeAt(i)
            }
        }
        val isCollisionAnimating = Particle.hasActiveVisibleCollisionParticles(particleList, viewPort)
        if (playerIsColliding && !isCollisionAnimating) {
            applyCollisionAndMapItemReturnParticles(particles, player, particleList, gameMap)
        }
        val adjustedTargetRect = targetRect?.inflate(ShooterEnemy.PLAYER_PROXIMITY_THRESHOLD.toFloat())
        if (playerIsColliding && adjustedTargetRect != null) {
            physics.applyPlayerCollisionPhysics(player, adjustedTargetRect, viewPort)
        }
    }

    private fun applyCollisionAndMapItemReturnParticles(particles:Particles, player:Player, particleList:ArrayList<Particle>, gameMap:GameMap) {
        particles.applyCollisionParticles(player.x, player.y, particleList)
        if (scoreService.getTotal() != scoreService.getRemaining()) {
            val firstMapItem =
                gameMap.items.firstOrNull { it.type == ItemType.COLLECTABLE && it.state == GameElementState.INACTIVE }
                    ?: throw IllegalStateException("needs to be at least one map item")
            particles.applyMapItemReturnParticle(player, firstMapItem, particleList)
        }
    }

    private fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        val dx = x2 - x1
        val dy = y2 - y1
        return (dx * dx) + (dy * dy)
    }

}
