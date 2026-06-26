package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.CustomImageWrapper
import com.github.adamyork.sparrow.wasm.common.AudioQueue
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import com.github.adamyork.sparrow.wasm.data.*
import com.github.adamyork.sparrow.wasm.data.enemy.*
import com.github.adamyork.sparrow.wasm.data.item.CollectibleItem
import com.github.adamyork.sparrow.wasm.data.item.FinishItem
import com.github.adamyork.sparrow.wasm.data.item.ItemType
import com.github.adamyork.sparrow.wasm.data.map.GameMap
import com.github.adamyork.sparrow.wasm.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.Collision
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.engine.Physics
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.engine.data.ParticleType
import com.github.adamyork.sparrow.wasm.service.ScoreService
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.Point
import kotlin.math.pow
import kotlin.math.sqrt

@AppScope
@Inject
class DefaultCollision(
    private val physics: Physics,
    private val scoreService: ScoreService
) : Collision {

    companion object {
        const val COLLISION_COLOR_VALUE: Int = -16711906
    }

    private val logger = KotlinLogging.logger {}

    override lateinit var collisionImage: CustomImageWrapper

    lateinit var collisionBitmap: Bitmap

    override fun cacheCollisionPixels() {
        val image = Image.makeFromEncoded(collisionImage.bytes)
        collisionBitmap = Bitmap.makeFromImage(image)
    }

    override fun getCollisionBoundaries(player: Player): CollisionBoundaries {
        val left = findEdge(player.x, player, collisionImage, Direction.LEFT)
        val right = findEdge(player.x, player, collisionImage, Direction.RIGHT)
        val floor = findFloor(player.y, player, collisionImage)
        val ceiling = findCeiling(player.y, player, collisionImage)
        return CollisionBoundaries(left, right, ceiling, floor)
    }

    override fun recomputeXBoundaries(
        player: Player,
        previousBoundaries: CollisionBoundaries
    ): CollisionBoundaries {
        val left = findEdge(player.x, player, collisionImage, Direction.LEFT)
        val right = findEdge(player.x, player, collisionImage, Direction.RIGHT)
        return CollisionBoundaries(left, right, previousBoundaries.top, previousBoundaries.bottom)
    }

    override fun checkForItemCollision(
        player: Player,
        gameMap: GameMap,
        audioQueue: AudioQueue
    ): GameMap {
        var gameState = gameMap.state
        val managedMapItems = gameMap.items.map { item ->
            val itemRight = item.x + item.width
            val itemBottom = item.y + item.height
            val itemRect = Rect(
                item.x.toFloat(),
                item.y.toFloat(),
                itemRight.toFloat(),
                itemBottom.toFloat()
            )
            val playerRight = player.x + player.width
            val playerBottom = player.y + player.height
            val playerRect = Rect(
                player.x.toFloat(),
                player.y.toFloat(),
                playerRight.toFloat(),
                playerBottom.toFloat()
            )
            var nextItemState = item.state
            var nextFrameMetaData = item.frameMetadata
            if (playerRect.overlaps(itemRect) && nextItemState == GameElementState.ACTIVE) {
                if (item.type == ItemType.FINISH) {
                    logger.info { "finish reached" }
                    gameState = GameMapState.COMPLETED
                    nextItemState = GameElementState.INACTIVE
                } else {
                    logger.info { "item collision" }
                    nextItemState = GameElementState.DEACTIVATING
                    audioQueue.queue.add(Sounds.ITEM_COLLECT)
                    nextFrameMetaData = item.getFirstDeactivatingFrame()
                }
            }
            if (item.type == ItemType.FINISH) {
                (item as FinishItem).copy(state = nextItemState, frameMetadata = nextFrameMetaData)
            } else {
                (item as CollectibleItem).copy(state = nextItemState, frameMetadata = nextFrameMetaData)
            }
        }.toCollection(ArrayList())
        return gameMap.copy(state = gameState, items = managedMapItems)
    }

    override fun checkForEnemyCollisionAndProximity(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: AudioQueue,
        particles: Particles
    ): Pair<Player, GameMap> {
        val managedMapParticles = gameMap.particles
        var playerIsColliding = false
        var targetRect: Rect? = null
        val managedMapEnemies = gameMap.enemies.map { enemy ->
            var isColliding = false
            var isInteracting = false
            if ((enemy as GameElement).state != GameElementState.INACTIVE) {
                val enemyRight = enemy.x + enemy.width
                val enemyBottom = enemy.y + enemy.height
                val enemyRect = Rect(
                    enemy.x.toFloat(),
                    enemy.y.toFloat(),
                    enemyRight.toFloat(),
                    enemyBottom.toFloat()
                )
                val playerRight = player.x + player.width
                val playerBottom = player.y + player.height
                val playerRect = Rect(
                    player.x.toFloat(),
                    player.y.toFloat(),
                    playerRight.toFloat(),
                    playerBottom.toFloat()
                )
                if (playerRect.overlaps(enemyRect)) {
                    logger.info { "enemy collision !" }
                    targetRect = enemyRect
                    audioQueue.queue.add(Sounds.PLAYER_COLLISION)
                    val collisionParticles = particles.createCollisionParticles(enemy.x, enemy.y)
                    managedMapParticles.addAll(collisionParticles)
                    if (scoreService.getTotal() != scoreService.getRemaining()) {
                        val mapItemReturnParticle = particles.createMapItemReturnParticle(player)
                        managedMapParticles.add(mapItemReturnParticle)
                    }
                    isColliding = true
                    playerIsColliding = true
                }
                if (enemy.type == EnemyType.SHOOTER) {
                    val dist = distanceTo(
                        Point(player.x.toFloat(), player.y.toFloat()),
                        Point(enemy.x.toFloat(), enemy.y.toFloat())
                    )
                    if (dist <= ShooterEnemy.PLAYER_PROXIMITY_THRESHOLD) {
                        val managedProjectileParticlesResult =
                            particles.createProjectileParticle(player, enemy, gameMap.particles)
                        if (managedProjectileParticlesResult.second) {
                            logger.info { "enemy shoots" }
                            isInteracting = true
                            audioQueue.queue.add(Sounds.ENEMY_SHOOT)
                        }
                        managedMapParticles.addAll(managedProjectileParticlesResult.first)
                    }
                }
                val frameMetadataWithState = (enemy as GameElement).getNextFrameMetadataWithState()
                val metadata = frameMetadataWithState.first
                val metadataState = frameMetadataWithState.second
                if (isColliding) {
                    when (enemy.type) {
                        EnemyType.SHOOTER -> {
                            (enemy as ShooterEnemy).copy(
                                frameMetadata = metadata,
                                colliding = metadataState.colliding,
                                interacting = enemy.interacting
                            ) as Enemy
                        }

                        EnemyType.RUNNER -> {
                            (enemy as RunnerEnemy).copy(
                                frameMetadata = metadata,
                                colliding = GameElementCollisionState.COLLIDING,
                                interacting = enemy.interacting
                            ) as Enemy
                        }

                        else -> {
                            (enemy as BlockerEnemy).copy(
                                frameMetadata = metadata,
                                colliding = GameElementCollisionState.COLLIDING,
                                interacting = enemy.interacting
                            ) as Enemy
                        }
                    }
                } else if (isInteracting) {
                    when (enemy.type) {
                        EnemyType.SHOOTER -> {
                            (enemy as ShooterEnemy).copy(
                                frameMetadata = metadata,
                                colliding = enemy.colliding,
                                interacting = EnemyInteractionState.INTERACTING
                            ) as Enemy
                        }

                        EnemyType.RUNNER -> {
                            (enemy as RunnerEnemy).copy(
                                frameMetadata = metadata,
                                colliding = enemy.colliding,
                                interacting = EnemyInteractionState.INTERACTING
                            ) as Enemy
                        }

                        else -> {
                            (enemy as BlockerEnemy).copy(
                                frameMetadata = metadata,
                                colliding = enemy.colliding,
                                interacting = EnemyInteractionState.INTERACTING
                            ) as Enemy
                        }
                    }
                } else {
                    enemy
                }
            } else {
                enemy
            }
        }.toCollection(ArrayList())
        val nextPlayer: Player = if (playerIsColliding) {
            logger.info { "player is colliding apply physics" }
            physics.applyPlayerCollisionPhysics(player, targetRect, viewPort)
        } else {
            player
        }
        return Pair(
            nextPlayer,
            gameMap.copy(enemies = managedMapEnemies, particles = managedMapParticles)
        )
    }

    override fun checkForProjectileCollision(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: AudioQueue,
        particles: Particles
    ): Pair<Player, GameMap> {
        var playerIsColliding = false
        var targetRect: Rect? = null
        val managedMapParticles = gameMap.particles.map { particle ->
            if (particle.type == ParticleType.PROJECTILE) {
                val particleRight = particle.x + particle.width
                val particleBottom = particle.y + particle.height
                val particleRect = Rect(
                    particle.x.toFloat(),
                    particle.y.toFloat(),
                    particleRight.toFloat(),
                    particleBottom.toFloat()
                )
                val playerRight = player.x + player.width
                val playerBottom = player.y + player.height
                val playerRect = Rect(
                    player.x.toFloat(),
                    player.y.toFloat(),
                    playerRight.toFloat(),
                    playerBottom.toFloat()
                )
                var nextFrame = particle.frame
                if (playerRect.overlaps(particleRect)) {
                    logger.info { "particle collision !" }
                    targetRect = particleRect
                    audioQueue.queue.add(Sounds.PLAYER_COLLISION)
                    playerIsColliding = true
                    nextFrame = particle.lifetime
                }
                particle.copy(frame = nextFrame)
            } else {
                particle
            }
        }.toCollection(ArrayList())
        if (playerIsColliding) {
            val collisionParticles = particles.createCollisionParticles(player.x, player.y)
            managedMapParticles.addAll(collisionParticles)
            if (scoreService.getTotal() != scoreService.getRemaining()) {
                val mapItemReturnParticle = particles.createMapItemReturnParticle(player)
                managedMapParticles.add(mapItemReturnParticle)
            }
        }
        val nextPlayer: Player = if (playerIsColliding) {
            physics.applyPlayerCollisionPhysics(player, targetRect, viewPort)
        } else {
            player
        }
        return Pair(
            nextPlayer,
            gameMap.copy(enemies = gameMap.enemies, particles = managedMapParticles)
        )
    }

    private fun findFloor(
        startY: Int,
        player: Player,
        collisionImage: CustomImageWrapper
    ): Int {
        if (startY >= collisionImage.imageBitmap.height) {
            return collisionImage.imageBitmap.height - player.height
        }
        val normalizedX = player.x.coerceAtMost(collisionImage.imageBitmap.width - player.width)
        return if (testForColorCollision(normalizedX, startY, player.width, 1)) {
            startY - player.height
        } else {
            findFloor(startY + 1, player, collisionImage)
        }
    }

    private fun findCeiling(startY: Int, player: Player, collisionImage: CustomImageWrapper): Int {
        if (startY <= 0) {
            return 0
        }
        val normalizedX = player.x.coerceAtMost(collisionImage.imageBitmap.width - player.width)
        return if (testForColorCollision(normalizedX, startY, player.width, 1)) {
            startY + 1
        } else {
            findCeiling(startY - 1, player, collisionImage)
        }
    }

    private fun findEdge(
        startX: Int,
        player: Player,
        collisionImage: CustomImageWrapper,
        direction: Direction
    ): Int {
        if (startX < 0) {
            return 0
        }
        if (startX > collisionImage.imageBitmap.width - player.width) {
            return collisionImage.imageBitmap.width - player.width
        }
        return if (testForColorCollision(startX, player.y, 1, player.height)) {
            if (direction == Direction.RIGHT) {
                startX - player.width
            } else {
                startX
            }
        } else {
            if (direction == Direction.RIGHT) {
                findEdge(startX + 1, player, collisionImage, direction)
            } else {
                findEdge(startX - 1, player, collisionImage, direction)
            }
        }
    }

    private fun testForColorCollision(
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Boolean {
        try {
            val pixelMap = collisionBitmap.peekPixels() ?: return false
            for (y in y until (y + height)) {
                for (x in x until (x + width)) {
                    val pixelColor = pixelMap.getColor(x, y)
                    if (pixelColor == COLLISION_COLOR_VALUE) {
                        return true
                    }
                }
            }
            return false
        } catch (exception: Exception) {
            logger.warn { "ArrayIndexOutOfBoundsException x $x y:$y width:$width height:$height $exception" }
            return true
        }
    }

    private fun distanceTo(first: Point, second: Point): Int {
        return sqrt((second.x - first.x).pow(2) + (second.y - first.y).pow(2)).toInt()
    }
}
