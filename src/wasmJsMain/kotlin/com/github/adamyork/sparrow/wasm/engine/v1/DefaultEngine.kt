package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.graphics.asSkiaBitmap
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.common.data.*
import com.github.adamyork.sparrow.wasm.common.data.enemy.*
import com.github.adamyork.sparrow.wasm.common.data.item.*
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.common.data.player.PlayerJumpingState
import com.github.adamyork.sparrow.wasm.common.data.player.PlayerMovingState
import com.github.adamyork.sparrow.wasm.common.v1.DefaultAudioQueue
import com.github.adamyork.sparrow.wasm.engine.Collision
import com.github.adamyork.sparrow.wasm.engine.Engine
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.engine.Physics
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.engine.data.DrawResult
import com.github.adamyork.sparrow.wasm.engine.data.ParticleShape
import com.github.adamyork.sparrow.wasm.engine.data.ParticleType
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.ScoreService
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.*
import kotlin.math.ceil
import kotlin.math.floor


/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class DefaultEngine @AppScope @Inject constructor(
    private val physics: Physics,
    private val collision: Collision,
    private val particles: Particles,
    private val audioQueue: DefaultAudioQueue,
    private val scoreService: ScoreService,
    private val assetService: AssetService,
    private val statusProvider: StatusProvider
) : Engine {

    private val logger = KotlinLogging.logger {}

    private var mapItem: Item = NoOpItem()
    private var mapItemImage: Image = EmptyImage.createEmptyImage()
    private var playerImage: Image = EmptyImage.createEmptyImage()

    private val itemImageCache: HashMap<String, Image> = hashMapOf()
    private val enemyImageCache: HashMap<String, Image> = hashMapOf()

    private var foregroundSurface: Surface? = null

    private val mapElementPaint = Paint().apply { isAntiAlias = true }
    private val particlePaint = Paint().apply { isAntiAlias = false; mode = PaintMode.FILL }
    private val mapItemReturnPaint = Paint().apply { isAntiAlias = true }
    private val playerPaint = Paint().apply { isAntiAlias = true }

    private fun getOrCreateForegroundSurface(viewPort: ViewPort): Surface {
        if (foregroundSurface == null) {
            val imageInfo = ImageInfo.makeN32Premul(viewPort.width, viewPort.height)
            foregroundSurface = Surface.makeRaster(imageInfo)
        }
        return foregroundSurface!!
    }

    override fun initialize(gameMap: GameMap, collisionImageAndBytes: ImageAndBytes, player: Player) {
        this.collision.collisionImage = collisionImageAndBytes
        this.collision.cacheCollisionPixels()
        gameMap.items.forEach { item ->
            itemImageCache[item.type.name] = Image.makeFromBitmap(item.imageAndBytes.imageBitmap.asSkiaBitmap())
        }
        gameMap.enemies.forEach { enemy ->
            enemyImageCache[enemy.type.name] = Image.makeFromBitmap(enemy.imageAndBytes.imageBitmap.asSkiaBitmap())
        }
        mapItem = gameMap.items.firstOrNull() ?: NoOpItem()
        mapItemImage = mapItem.let {
            Image.makeFromBitmap(it.imageAndBytes.imageBitmap.asSkiaBitmap())
        }
        playerImage = Image.makeFromBitmap(player.imageAndBytes.imageBitmap.asSkiaBitmap())

    }

    override fun getCollisionBoundaries(player: Player): CollisionBoundaries {
        return collision.getCollisionBoundaries(player)
    }

    override fun managePlayer(player: Player, collisionBoundaries: CollisionBoundaries): Player {
        val physicsAppliedPlayer = physics.applyPlayerPhysics(player, collisionBoundaries, collision)
        val nextFrameMetadataWithState = physicsAppliedPlayer.getNextFrameMetadataWithState()
        val metadata = nextFrameMetadataWithState.first
        val metadataState = nextFrameMetadataWithState.second
        return physicsAppliedPlayer.copy(frameMetadata = metadata, colliding = metadataState.colliding)
    }

    override fun manageViewport(player: Player, viewPort: ViewPort): ViewPort {
        val nextX = when (player.direction) {
            Direction.RIGHT -> {
                val adjustedX = player.x + player.width
                val viewPortRightBoundary = viewPort.x + viewPort.width
                if (adjustedX > viewPortRightBoundary) {
                    logger.debug { "move map horizontal right" }
                    (viewPort.x + (adjustedX - viewPortRightBoundary))
                        .coerceAtMost(collision.collisionImage.imageBitmap.width - viewPort.width)
                } else viewPort.x
            }

            Direction.LEFT -> {
                if (player.x < viewPort.x) {
                    logger.debug { "move map horizontal left" }
                    (viewPort.x - (viewPort.x - player.x))
                        .coerceAtLeast(0)
                } else viewPort.x
            }
        }
        val nextY = when {
            player.y < viewPort.y -> {
                logger.debug { "move map vertical up" }
                (viewPort.y - (viewPort.y - player.y))
                    .coerceAtLeast(0)
            }

            (player.y + player.height) > (viewPort.y + viewPort.height) -> {
                logger.debug { "move map vertical down" }
                (viewPort.y + (player.y - viewPort.y))
                    .coerceAtMost(collision.collisionImage.imageBitmap.height - viewPort.height)
            }

            else -> viewPort.y
        }
        val nextViewPort = ViewPort(nextX, nextY, viewPort.x, viewPort.y, viewPort.width, viewPort.height)
        if (nextX != viewPort.x || nextY != viewPort.y) {
            logger.debug { "viewport has changed $nextViewPort" }
        }
        return nextViewPort
    }

    override fun manageMap(player: Player, gameMap: GameMap, viewPort: ViewPort): GameMap {
        val managedMapItems = manageMapItems(gameMap)
        val managedMapEnemies = manageMapEnemies(gameMap, player)
        val managedCollisionParticles = physics.applyCollisionParticlePhysics(gameMap.particles, viewPort)
        val managedMapItemReturnParticles = physics.applyMapItemReturnParticlePhysics(managedCollisionParticles)
        if (player.moving == PlayerMovingState.MOVING && player.jumping == PlayerJumpingState.GROUNDED) {
            val nextDustParticles = particles.createDustParticles(player)
            managedMapItemReturnParticles.addAll(nextDustParticles)
        }
        val managedDustParticles = physics.applyDustParticlePhysics(managedMapItemReturnParticles)
        val managedAllParticles = physics.applyProjectileParticlePhysics(managedDustParticles, viewPort)
        var mapState = gameMap.state
        if (mapState == GameMapState.COLLECTING && scoreService.allFound()) {
            mapState = GameMapState.COMPLETING
        }
        return gameMap.copy(
            state = mapState,
            items = managedMapItems,
            enemies = managedMapEnemies,
            particles = managedAllParticles
        )
    }

    override fun manageEnemyAndItemCollision(
        player: Player,
        map: GameMap,
        viewPort: ViewPort
    ): Pair<Player, GameMap> {
        val nextMap = collision.checkForItemCollision(player, map, audioQueue)
        val enemyCollisionResult =
            collision.checkForEnemyCollisionAndProximity(player, nextMap, viewPort, audioQueue, particles)
        val projectTileCollisionResult =
            collision.checkForProjectileCollision(
                enemyCollisionResult.first,
                enemyCollisionResult.second,
                viewPort,
                audioQueue,
                particles
            )
        var nextGameMap = projectTileCollisionResult.second
        if (projectTileCollisionResult.first.colliding == GameElementCollisionState.COLLIDING) {
            val nextItems = returnMapItemAfterCollision(nextGameMap)
            nextGameMap = nextGameMap.copy(items = nextItems)
        }
        return Pair(projectTileCollisionResult.first, nextGameMap)
    }

    private fun manageMapItems(gameMap: GameMap): ArrayList<Item> {
        for ((index, item) in gameMap.items.withIndex()) {
            val itemX = item.x
            val itemY = item.y
            val frameMetadataWithState = (item as GameElement).getNextFrameMetadataWithState()
            val metadata = frameMetadataWithState.first
            var nextState = frameMetadataWithState.second.state
            if (item.type == ItemType.FINISH) {
                if (gameMap.state == GameMapState.COMPLETING && item.state == GameElementState.INACTIVE) {
                    nextState = GameElementState.ACTIVE
                }
            }
            if (item.type == ItemType.FINISH) {
                gameMap.items[index] =
                    (item as FinishItem).copy(x = itemX, y = itemY, state = nextState, frameMetadata = metadata)
            } else {
                gameMap.items[index] = (item as CollectibleItem).copy(
                    x = itemX,
                    y = itemY,
                    state = nextState,
                    frameMetadata = metadata
                )
            }
        }
        return gameMap.items
    }

    private fun returnMapItemAfterCollision(gameMap: GameMap): ArrayList<Item> {
        val index = gameMap.items.indexOfFirst { item ->
            item.type == ItemType.COLLECTABLE && item.state == GameElementState.INACTIVE
        }
        if (index != -1) {
            val item = gameMap.items[index]
            if (item is CollectibleItem) {
                gameMap.items[index] = item.copy(state = GameElementState.ACTIVE)
            }
        }
        return gameMap.items
    }

    private fun manageMapEnemies(gameMap: GameMap, player: Player): ArrayList<Enemy> {
        val deltaTimeCoefficient = statusProvider.getDeltaTimeCoefficient()
        for ((index, enemy) in gameMap.enemies.withIndex()) {
            val nextState = enemy.getNextEnemyState(player)
            if (nextState != GameElementState.INACTIVE) {
                val nextPosition = when (enemy) {
                    is BlockerEnemy -> {
                        enemy.getNextPosition(deltaTimeCoefficient)
                    }

                    is RunnerEnemy -> {
                        enemy.getNextPosition(deltaTimeCoefficient)
                    }

                    else -> {
                        enemy.getNextPosition()
                    }
                }
                val itemX = nextPosition.x
                val itemY = nextPosition.y
                val frameMetadataWithState = (enemy as GameElement).getNextFrameMetadataWithState()
                val metadata = frameMetadataWithState.first
                val metadataState = frameMetadataWithState.second
                when (enemy.type) {
                    EnemyType.SHOOTER -> {
                        gameMap.enemies[index] = (enemy as ShooterEnemy).copy(
                            x = itemX,
                            y = itemY,
                            state = nextState,
                            frameMetadata = metadata,
                            enemyPosition = nextPosition,
                            colliding = metadataState.colliding,
                            interacting = metadataState.interacting
                        )
                    }

                    EnemyType.RUNNER -> {
                        gameMap.enemies[index] = (enemy as RunnerEnemy).copy(
                            x = itemX,
                            y = itemY,
                            state = nextState,
                            frameMetadata = metadata,
                            enemyPosition = nextPosition,
                            colliding = metadataState.colliding,
                            interacting = metadataState.interacting
                        )
                    }

                    else -> {
                        gameMap.enemies[index] = (enemy as BlockerEnemy).copy(
                            x = itemX,
                            y = itemY,
                            state = nextState,
                            frameMetadata = metadata,
                            enemyPosition = nextPosition,
                            colliding = metadataState.colliding,
                            interacting = metadataState.interacting
                        )
                    }
                }
            } else if (enemy.type == EnemyType.RUNNER) {
                gameMap.enemies[index] = (enemy as RunnerEnemy).copy(state = nextState)
            } else {
                gameMap.enemies[index] = enemy
            }
        }
        return gameMap.enemies
    }

    override fun draw(
        map: GameMap,
        viewPort: ViewPort,
        player: Player,
        timestamp: Double
    ): DrawResult {
        val foregroundSurface = getOrCreateForegroundSurface(viewPort)
        val foregroundCanvas = foregroundSurface.canvas
        foregroundCanvas.clear(0x00000000)
        drawMapElements(
            map.items,
            viewPort,
            foregroundCanvas,
            transformDirection = false
        )
        drawMapElements(
            map.enemies,
            viewPort,
            foregroundCanvas,
            transformDirection = true
        )
        drawParticles(map, viewPort, foregroundCanvas, mapItem, mapItemImage)
        drawPlayer(player, viewPort, foregroundCanvas, playerImage)
        val foregroundImage = foregroundSurface.makeImageSnapshot()
        statusProvider.lastPaintTime = timestamp
        return DrawResult(
            foregroundImage = foregroundImage,
            foregroundOffsetX = viewPort.x.toFloat(),
            foregroundOffsetY = viewPort.y.toFloat(),
            farGroundBitmap = map.farGroundAsset.imageAndBytes.imageBitmap,
            farGroundOffsetX = map.getFarGroundX(viewPort).toFloat(),
            farGroundOffsetY = viewPort.y.toFloat(),
            midGroundBitmap = map.midGroundAsset.imageAndBytes.imageBitmap,
            midGroundOffsetX = map.getMidGroundX(viewPort).toFloat(),
            midGroundOffsetY = viewPort.y.toFloat(),
            collisionBitmap = if (assetService.showCollisionMap()) map.collisionAsset.imageBitmap else null,
            collisionOffsetX = viewPort.x.toFloat(),
            collisionOffsetY = viewPort.y.toFloat(),
        )
    }

    override fun createDefaultPlayer(playerAsset: ImageAsset): Player {
        return Player(
            assetService.gameConfig.player.x,
            assetService.gameConfig.player.y,
            playerAsset.width,
            playerAsset.height,
            GameElementState.ACTIVE,
            FrameMetadata(1, Cell(1, 1, playerAsset.width, playerAsset.height)),
            playerAsset.imageAndBytes,
            0.0,
            0.0,
            PlayerJumpingState.GROUNDED,
            PlayerMovingState.STATIONARY,
            Direction.RIGHT,
            GameElementCollisionState.FREE,
            0,
            assetService.gameConfig.engine.fps.animation.toDouble()
        )
    }

    override fun startInput(controlAction: ControlAction, player: Player): Player {
        when (controlAction) {
            ControlAction.LEFT, ControlAction.RIGHT -> {
                val direction = if (controlAction == ControlAction.LEFT) Direction.LEFT else Direction.RIGHT
                return player.copy(
                    moving = PlayerMovingState.MOVING,
                    direction = direction,
                    vx = adjustXVelocity(controlAction, player)
                )
            }

            ControlAction.JUMP -> {
                if (player.jumping == PlayerJumpingState.GROUNDED) {
                    audioQueue.queue.add(Sounds.JUMP)
                    return player.copy(jumping = PlayerJumpingState.INITIAL)
                }
            }
        }
        return player
    }

    private fun adjustXVelocity(controlAction: ControlAction, player: Player): Double {
        val movingLeft = controlAction == ControlAction.LEFT
        val movingRight = controlAction == ControlAction.RIGHT
        val isChangingToLeft = movingLeft && player.direction == Direction.RIGHT
        val isChangingToRight = movingRight && player.direction == Direction.LEFT
        val isChangingDirection = isChangingToLeft || isChangingToRight
        return if (isChangingDirection) {
            logger.info { getDirectionChangedLogMessage(player) }
            0.0
        } else {
            player.vx
        }
    }

    private fun getDirectionChangedLogMessage(player: Player): String {
        return "direction changed player vx was: ${player.vx} and is now 0"
    }

    override fun stopInput(controlAction: ControlAction, player: Player): Player {
        val movingLeft = controlAction == ControlAction.LEFT
        val movingRight = controlAction == ControlAction.RIGHT
        if (movingLeft && player.direction == Direction.RIGHT) {
            logger.warn { "stop player left called before right started" }
        } else if (movingRight && player.direction == Direction.LEFT) {
            logger.warn { "stop player right called before left started" }
        }
        val isStoppingLeft = movingLeft && player.direction == Direction.LEFT
        val isStoppingRight = movingRight && player.direction == Direction.RIGHT
        val matchesDirection = isStoppingLeft || isStoppingRight
        if (matchesDirection) {
            return player.copy(moving = PlayerMovingState.STATIONARY)
        }
        return player
    }

    private fun drawPlayer(
        player: Player,
        viewPort: ViewPort,
        canvas: Canvas,
        image: Image
    ) {
        val localCord = viewPort.globalToLocal(player.x, player.y)
        val shouldShowTint = player.immunityTicks > 0 && (player.immunityTicks / 8) % 2 == 0
        playerPaint.colorFilter = if (shouldShowTint) {
            ColorFilter.makeBlend(0x8000FF00.toInt(), BlendMode.SRC_ATOP)
        } else {
            null
        }
        drawTransformed(canvas, localCord, player.width, player.height, player.direction == Direction.LEFT) {
            drawSprite(canvas, image, player, localCord, playerPaint)
        }
    }

    private fun drawParticles(map: GameMap, viewPort: ViewPort, canvas: Canvas, mapItem: Item?, mapItemImage: Image?) {
        map.particles.forEach { particle ->
            val localCord = viewPort.globalToLocal(particle.x, particle.y)
            if (particle.type == ParticleType.MAP_ITEM_RETURN && mapItem != null && mapItemImage != null) {
                canvas.drawImageRect(
                    image = mapItemImage,
                    src = Rect.makeXYWH(0f, 0f, mapItem.width.toFloat(), mapItem.height.toFloat()),
                    dst = Rect.makeXYWH(
                        localCord.first.toFloat(),
                        localCord.second.toFloat(),
                        particle.width.toFloat(),
                        particle.height.toFloat()
                    ),
                    samplingMode = SamplingMode.LINEAR,
                    paint = mapItemReturnPaint,
                    strict = true
                )
            } else {
                val lifetime = particle.lifetime.coerceAtLeast(1)
                val ageProgress = (particle.frame.toFloat() / lifetime.toFloat()).coerceIn(0f, 1f)
                val lifeAlphaMultiplier = if (particle.type == ParticleType.PROJECTILE) 1f else 1f - ageProgress
                val particleAlpha =
                    (particle.color.alpha.coerceIn(0f, 1f) * lifeAlphaMultiplier * 255f).toInt().coerceIn(0, 255)
                val particleRed = (particle.color.red.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
                val particleGreen = (particle.color.green.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
                val particleBlue = (particle.color.blue.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
                particlePaint.color = Color.makeARGB(particleAlpha, particleRed, particleGreen, particleBlue)
                val left = floor(localCord.first.toDouble()).toFloat()
                val top = floor(localCord.second.toDouble()).toFloat()
                val width = ceil(particle.width.toDouble()).toFloat()
                val height = ceil(particle.height.toDouble()).toFloat()
                val rect = Rect.makeXYWH(left, top, width, height)
                if (particle.shape == ParticleShape.CIRCLE) {
                    canvas.drawOval(rect, particlePaint)
                } else {
                    canvas.drawRect(rect, particlePaint)
                }
            }
        }
    }

    private fun drawMapElements(
        elements: ArrayList<out GameElement>,
        viewPort: ViewPort,
        canvas: Canvas,
        transformDirection: Boolean
    ) {
        elements.forEach { element ->
            if (element.state != GameElementState.INACTIVE && element.cullingCheck(viewPort)) {
                val localCord = viewPort.globalToLocal(element.x, element.y)
                val elementImage = when (element) {
                    is Enemy -> enemyImageCache[element.type.name]
                    is Item -> itemImageCache[element.type.name]
                    else -> null
                } ?: throw IllegalStateException("No image found for element")

                val flip = transformDirection && element.nestedDirection() == Direction.LEFT
                drawTransformed(canvas, localCord, element.width, element.height, flip) {
                    drawSprite(canvas, elementImage, element, localCord, mapElementPaint)
                }
            }
        }
    }

    private fun drawTransformed(
        canvas: Canvas,
        localCord: Pair<Int, Int>,
        width: Int,
        height: Int,
        flip: Boolean,
        drawAction: () -> Unit
    ) {
        canvas.save()
        try {
            if (flip) {
                val pivotX = localCord.first + (width / 2f)
                val pivotY = localCord.second + (height / 2f)
                canvas.translate(pivotX, pivotY)
                canvas.scale(-1f, 1f)
                canvas.translate(-pivotX, -pivotY)
            }
            drawAction()
        } finally {
            canvas.restore()
        }
    }

    private fun drawSprite(
        canvas: Canvas,
        image: Image,
        element: GameElement,
        localCord: Pair<Int, Int>,
        paint: Paint
    ) {
        val srcX = element.frameMetadata.cell.x.toFloat()
        val srcY = element.frameMetadata.cell.y.toFloat()
        val w = element.width.toFloat()
        val h = element.height.toFloat()
        val dstX = localCord.first.toFloat()
        val dstY = localCord.second.toFloat()
        canvas.drawImageRect(
            image = image,
            srcLeft = srcX,
            srcTop = srcY,
            srcRight = srcX + w,
            srcBottom = srcY + h,
            dstLeft = dstX,
            dstTop = dstY,
            dstRight = dstX + w,
            dstBottom = dstY + h,
            samplingMode = SamplingMode.DEFAULT,
            paint = paint,
            strict = true
        )
    }
}