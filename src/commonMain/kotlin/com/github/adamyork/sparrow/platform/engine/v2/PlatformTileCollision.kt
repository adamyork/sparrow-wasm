package com.github.adamyork.sparrow.platform.engine.v2

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.AudioQueue
import com.github.adamyork.sparrow.platform.common.data.*
import com.github.adamyork.sparrow.platform.common.data.enemy.EnemyInteractionState
import com.github.adamyork.sparrow.platform.common.data.enemy.EnemyType
import com.github.adamyork.sparrow.platform.common.data.enemy.ShooterEnemy
import com.github.adamyork.sparrow.platform.common.data.item.ItemType
import com.github.adamyork.sparrow.platform.common.data.map.GameMap
import com.github.adamyork.sparrow.platform.common.data.map.GameMapState
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.engine.Collision
import com.github.adamyork.sparrow.platform.engine.Particles
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.platform.engine.data.Particle
import com.github.adamyork.sparrow.platform.engine.data.ParticleType
import com.github.adamyork.sparrow.platform.service.ScoreService
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject
import kotlin.math.absoluteValue

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
abstract class PlatformTileCollision(
    private val physics: Physics,
    private val scoreService: ScoreService
) : Collision {

    companion object {
        const val COLLISION_COLOR_VALUE: Int = -16711906
        private const val TILE_SIZE: Int = 16
        private const val SOLID_THRESHOLD_RATIO: Float = 0.20f // Lowered slightly to ensure thin lines/edges catch
        private const val SHOOTER_PROXIMITY_THRESHOLD_SQUARED: Int =
            ShooterEnemy.PLAYER_PROXIMITY_THRESHOLD * ShooterEnemy.PLAYER_PROXIMITY_THRESHOLD
    }

    private val logger = KotlinLogging.logger {}

    override lateinit var collisionImage: ImageAndBytes
    protected var bitmapWidth: Int = 0
    protected var bitmapHeight: Int = 0

    private var tilesColumns: Int = 0
    private var tilesRows: Int = 0
    private lateinit var solidTileMap: BooleanArray

    private var lastPlayerX: Int = -1
    private var lastPlayerY: Int = -1
    private var cachedBoundaries: CollisionBoundaries? = null

    override fun cacheCollisionPixels() {
        bitmapWidth = collisionImage.imageBitmap.width
        bitmapHeight = collisionImage.imageBitmap.height
        tilesColumns = (bitmapWidth + TILE_SIZE - 1) / TILE_SIZE
        tilesRows = (bitmapHeight + TILE_SIZE - 1) / TILE_SIZE
        solidTileMap = BooleanArray(tilesColumns * tilesRows)

        val bytes = collisionImage.bytes

        for (row in 0 until tilesRows) {
            for (col in 0 until tilesColumns) {
                val startX = col * TILE_SIZE
                val startY = row * TILE_SIZE
                val endX = (startX + TILE_SIZE).coerceAtMost(bitmapWidth)
                val endY = (startY + TILE_SIZE).coerceAtMost(bitmapHeight)

                var matchCount = 0
                val totalPixelsInTile = (endX - startX) * (endY - startY)

                for (y in startY until endY) {
                    val rowOffset = y * bitmapWidth
                    for (x in startX until endX) {
                        val pixelIndex = (rowOffset + x) * 4
                        if (pixelIndex + 3 < bytes.size) {
                            val b1 = bytes[pixelIndex].toInt() and 0xFF
                            val b2 = bytes[pixelIndex + 1].toInt() and 0xFF
                            val b3 = bytes[pixelIndex + 2].toInt() and 0xFF
                            val b4 = bytes[pixelIndex + 3].toInt() and 0xFF
                            val pixelColor = (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1

                            if (pixelColor == COLLISION_COLOR_VALUE) {
                                matchCount++
                            }
                        }
                    }
                }

                val density = matchCount.toFloat() / totalPixelsInTile
                solidTileMap[row * tilesColumns + col] = density >= SOLID_THRESHOLD_RATIO
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
            findEdgeTile(player.x, player, Direction.LEFT),
            findEdgeTile(player.x, player, Direction.RIGHT),
            findCeilingTile(player.y, player),
            findFloorTile(player.y, player)
        )
        return cachedBoundaries!!
    }

    override fun updateCollisionXBoundaries(
        player: Player,
        collisionBoundaries: CollisionBoundaries
    ) {
        // Intentionally left as a no-op per requirements
    }

    private fun findFloorTile(startY: Int, player: Player): Int {
        val playerBottom = startY + player.height
        val startTileY = playerBottom / TILE_SIZE
        val endTileY = (bitmapHeight - 1) / TILE_SIZE

        for (tileY in startTileY..endTileY) {
            val tileTopY = tileY * TILE_SIZE
            if (testTileCollision(player.x, tileTopY, player.width, 1)) {
                return tileTopY - player.height
            }
        }
        return bitmapHeight - player.height
    }

    private fun findCeilingTile(startY: Int, player: Player): Int {
        val startTileY = startY / TILE_SIZE
        for (tileY in startTileY downTo 0) {
            val tileBottomY = (tileY + 1) * TILE_SIZE
            if (testTileCollision(player.x, tileY * TILE_SIZE, player.width, 1)) {
                return tileBottomY
            }
        }
        return 0
    }

    private fun findEdgeTile(startX: Int, player: Player, direction: Direction): Int {
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
            if (testTileCollision(checkX.coerceIn(0, bitmapWidth - 1), player.y, 1, player.height)) {
                return if (direction == Direction.RIGHT) x - 1 else x + 1
            }
        }
        val endPosition = if (direction == Direction.RIGHT) startX + maxLookAhead else startX - maxLookAhead
        return endPosition.coerceIn(0, maxPossibleX)
    }

    private fun testTileCollision(x: Int, y: Int, width: Int, height: Int): Boolean {
        if (x < 0 || y < 0 || x + width > bitmapWidth || y + height > bitmapHeight) return true

        val startCol = x / TILE_SIZE
        val endCol = (x + width - 1) / TILE_SIZE
        val startRow = y / TILE_SIZE
        val endRow = (y + height - 1) / TILE_SIZE

        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                if (row in 0 until tilesRows && col in 0 until tilesColumns) {
                    if (solidTileMap[row * tilesColumns + col]) {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun applyAllItemCollision(player: Player, gameMap: GameMap, audioQueue: AudioQueue) {
        val items = gameMap.items
        val playerRect = player.toRect()
        var newGameState = gameMap.state
        for (itemIndex in items.indices) {
            val item = items[itemIndex]
            if (item.state == ElementState.ACTIVE && playerRect.overlaps(item.toRect())) {
                if (item.type == ItemType.FINISH) {
                    newGameState = GameMapState.COMPLETED
                }
                audioQueue.queue.add(Sounds.ITEM_COLLECT)
                item.state = ElementState.DEACTIVATING
                item.frameMetadata = item.getFirstDeactivatingFrame()
                items[itemIndex] = item
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
        for (enemyIndex in managedMapEnemies.indices) {
            val enemy = managedMapEnemies[enemyIndex]
            val element = enemy as GameElement
            if (element.state == ElementState.INACTIVE) continue
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
            managedMapEnemies[enemyIndex] = enemy
        }
        if (playerIsColliding && closestEnemyRect != null) {
            logger.info { "Player collision detected with enemy at (${closestEnemyRect.left}, ${closestEnemyRect.top})" }
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
        for (particleIndex in particleList.indices.reversed()) {
            val particle = particleList[particleIndex]
            if (canTakeCollisionDamage &&
                particle.type == ParticleType.PROJECTILE &&
                playerRect.overlaps(particle.toRect())
            ) {
                targetRect = particle.toRect()
                audioQueue.queue.add(Sounds.PLAYER_COLLISION)
                playerIsColliding = true
                particleList.removeAt(particleIndex)
            }
        }
        val isCollisionAnimating = Particle.hasActiveVisibleCollisionParticles(particleList, viewPort)
        if (playerIsColliding && !isCollisionAnimating) {
            applyCollisionAndMapItemReturnParticles(particles, player, particleList, gameMap)
        }
        val adjustedTargetRect = targetRect?.inflate(ShooterEnemy.PLAYER_PROXIMITY_THRESHOLD.toFloat())
        if (playerIsColliding && adjustedTargetRect != null) {
            logger.info { "Player collision detected with projectile at (${adjustedTargetRect.left}, ${adjustedTargetRect.top})" }
            physics.applyPlayerCollisionPhysics(player, adjustedTargetRect, viewPort)
        }
    }

    private fun applyCollisionAndMapItemReturnParticles(
        particles: Particles,
        player: Player,
        particleList: ArrayList<Particle>,
        gameMap: GameMap
    ) {
        particles.applyCollisionParticles(player.x, player.y, particleList)
        if (scoreService.getTotal() != scoreService.getRemaining()) {
            val firstMapItem =
                gameMap.items.firstOrNull { it.type == ItemType.COLLECTABLE && it.state == ElementState.INACTIVE }
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