package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.graphics.asSkiaBitmap
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.common.data.*
import com.github.adamyork.sparrow.wasm.common.data.enemy.BlockerEnemy
import com.github.adamyork.sparrow.wasm.common.data.enemy.Enemy
import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyType
import com.github.adamyork.sparrow.wasm.common.data.enemy.RunnerEnemy
import com.github.adamyork.sparrow.wasm.common.data.item.CollectibleItem
import com.github.adamyork.sparrow.wasm.common.data.item.Item
import com.github.adamyork.sparrow.wasm.common.data.item.ItemType
import com.github.adamyork.sparrow.wasm.common.data.item.DefaultItem
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
import com.github.adamyork.sparrow.wasm.engine.data.*
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.ScoreService
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.*


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

    private var mapItem: Item = DefaultItem()
    private var mapItemImage: Image = EmptyImage.createEmptyImage()
    private var playerImage: Image = EmptyImage.createEmptyImage()

    private val itemImageCache: HashMap<String, Image> = hashMapOf()
    private val enemyImageCache: HashMap<String, Image> = hashMapOf()
    private val flippedFrameCache: HashMap<String, Image> = hashMapOf()

    private var foregroundSurface: Surface? = null

    private val mapElementPaint = Paint().apply { isAntiAlias = true }
    private val particlePaint = Paint().apply { isAntiAlias = false; mode = PaintMode.FILL }
    private val mapItemReturnPaint = Paint().apply { isAntiAlias = true }
    private val playerPaintNormal = Paint().apply { isAntiAlias = true }
    private val playerPaintTinted = Paint().apply {
        isAntiAlias = true
        colorFilter = ColorFilter.makeBlend(0x8000FF00.toInt(), BlendMode.SRC_ATOP)
    }

    private fun getOrCreateForegroundSurface(viewPort: ViewPort): Surface =
        foregroundSurface ?: Surface.makeRaster(ImageInfo.makeN32Premul(viewPort.width, viewPort.height)).also {
            foregroundSurface = it
        }

    override fun initialize(gameMap: GameMap, collisionImageAndBytes: ImageAndBytes, player: Player) {
        flippedFrameCache.values.forEach { it.close() }
        flippedFrameCache.clear()
        this.collision.collisionImage = collisionImageAndBytes
        this.collision.cacheCollisionPixels()
        gameMap.items.forEach { item ->
            itemImageCache[item.type.name] = Image.makeFromBitmap(item.imageAndBytes.imageBitmap.asSkiaBitmap())
        }
        gameMap.enemies.forEach { enemy ->
            enemyImageCache[enemy.type.name] = Image.makeFromBitmap(enemy.imageAndBytes.imageBitmap.asSkiaBitmap())
        }
        mapItem = gameMap.items.firstOrNull() ?: DefaultItem()
        mapItemImage = Image.makeFromBitmap(mapItem.imageAndBytes.imageBitmap.asSkiaBitmap())
        playerImage = Image.makeFromBitmap(player.imageAndBytes.imageBitmap.asSkiaBitmap())

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
        viewPort.x = nextX
        viewPort.y = nextY
        viewPort.lastX = viewPort.x
        viewPort.lastY = viewPort.y
    }

    override fun manageMap(player: Player, gameMap: GameMap, viewPort: ViewPort) {
        manageMapItems(gameMap)
        manageMapEnemies(gameMap, player)
        physics.applyCollisionParticlePhysics(gameMap.particles, viewPort)
        physics.applyMapItemReturnParticlePhysics(gameMap.particles)
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

    override fun manageEnemyAndItemCollision(
        player: Player,
        map: GameMap,
        viewPort: ViewPort
    ) {
        collision.applyAllItemCollision(player, map, audioQueue)
        collision.applyEnemyAndProximityCollision(player, map, viewPort, audioQueue, particles)
        collision.applyProjectileCollision(
            player,
            map,
            viewPort,
            audioQueue,
            particles
        )
        if (player.colliding == GameElementCollisionState.COLLIDING) {
            adjustMapAfterItemCollision(map)
        }
    }

    private fun manageMapItems(gameMap: GameMap) {
        for ((index, item) in gameMap.items.withIndex()) {
            val (metadata, metadataState) = (item as GameElement).getNextFrameMetadataWithState()
            var nextState = metadataState.state
            if (item.type == ItemType.FINISH) {
                if (gameMap.state == GameMapState.COMPLETING && item.state == GameElementState.INACTIVE) {
                    nextState = GameElementState.ACTIVE
                } else if (gameMap.state == GameMapState.COLLECTING && item.state != GameElementState.INACTIVE) {
                    nextState = GameElementState.INACTIVE
                }
            }
            item.state = nextState
            item.frameMetadata = metadata
            gameMap.items[index] = item
        }
    }

    private fun adjustMapAfterItemCollision(gameMap: GameMap) {
        val index = gameMap.items.indexOfFirst { item ->
            item.type == ItemType.COLLECTABLE && item.state == GameElementState.INACTIVE
        }
        if (index != -1) {
            val item = gameMap.items[index]
            if (item is CollectibleItem) {
                item.state = GameElementState.ACTIVE
                gameMap.items[index] = item
                if (gameMap.state == GameMapState.COMPLETING) {
                    gameMap.state = GameMapState.COLLECTING
                }
            }
        }
    }

    private fun manageMapEnemies(gameMap: GameMap, player: Player) {
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

        val hasVisibleEnemyCandidates = hasVisibleActiveElements(map.enemies, viewPort)
        if (hasVisibleEnemyCandidates) {
            drawMapElements(
                map.enemies,
                viewPort,
                foregroundCanvas,
                transformDirection = true
            )
        }

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

    override fun startInput(controlAction: ControlAction, player: Player) {
        when (controlAction) {
            ControlAction.LEFT, ControlAction.RIGHT -> {
                val direction = if (controlAction == ControlAction.LEFT) Direction.LEFT else Direction.RIGHT
                player.moving = PlayerMovingState.MOVING
                player.direction = direction
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
        if (movingLeft && player.direction == Direction.RIGHT) {
            logger.warn { "stop player left called before right started" }
        } else if (movingRight && player.direction == Direction.LEFT) {
            logger.warn { "stop player right called before left started" }
        }
        val isStoppingLeft = movingLeft && player.direction == Direction.LEFT
        val isStoppingRight = movingRight && player.direction == Direction.RIGHT
        val matchesDirection = isStoppingLeft || isStoppingRight
        if (matchesDirection) {
            player.moving = PlayerMovingState.STATIONARY
        }
    }

    private fun drawPlayer(
        player: Player,
        viewPort: ViewPort,
        canvas: Canvas,
        image: Image
    ) {
        val localX = player.x - viewPort.x
        val localY = player.y - viewPort.y
        val shouldShowTint = player.immunityTicks > 0 && (player.immunityTicks / 8) % 2 == 0
        val activePlayerPaint = if (shouldShowTint) playerPaintTinted else playerPaintNormal

        val flip = player.direction == Direction.LEFT
        if (flip) {
            drawFlippedSprite(canvas, image, player, localX, localY, activePlayerPaint)
        } else {
            drawSprite(canvas, image, player, localX, localY, activePlayerPaint)
        }
    }

    private fun drawParticles(
        map: GameMap,
        viewPort: ViewPort,
        canvas: Canvas,
        mapItem: Item?,
        mapItemImage: Image?
    ) {
        val vpX = viewPort.x.toFloat()
        val vpY = viewPort.y.toFloat()
        val groups = mutableMapOf<Int, MutableList<Particle>>()
        for (particle in map.particles) {
            if (!particle.cullingCheck(viewPort)) {
                continue
            }
            if (particle.type == ParticleType.MAP_ITEM_RETURN) {
                if (mapItem != null && mapItemImage != null) {
                    val localX = particle.x.toFloat() - vpX
                    val localY = particle.y.toFloat() - vpY
                    canvas.drawImageRect(
                        image = mapItemImage,
                        srcLeft = 0f, srcTop = 0f,
                        srcRight = mapItem.width.toFloat(), srcBottom = mapItem.height.toFloat(),
                        dstLeft = localX, dstTop = localY,
                        dstRight = localX + particle.width.toFloat(),
                        dstBottom = localY + particle.height.toFloat(),
                        samplingMode = SamplingMode.LINEAR,
                        paint = mapItemReturnPaint,
                        strict = true
                    )
                }
                continue
            }
            val lifetime = if (particle.lifetime <= 0) 1 else particle.lifetime
            val ageProgress = (particle.frame.toFloat() / lifetime.toFloat()).coerceIn(0f, 1f)
            val alphaMultiplier = when {
                particle.type == ParticleType.PROJECTILE -> 1.0f
                ageProgress < 0.33f -> 1.0f
                ageProgress < 0.66f -> 0.66f
                else -> 0.33f
            }
            val alpha = (particle.color.alpha.coerceIn(0f, 1f) * alphaMultiplier * 255f).toInt().coerceIn(0, 255)
            val color = Color.makeARGB(
                alpha,
                (particle.color.red * 255).toInt(),
                (particle.color.green * 255).toInt(),
                (particle.color.blue * 255).toInt()
            )
            groups.getOrPut(color) { mutableListOf() }.add(particle)
        }
        for ((color, particleList) in groups) {
            val builder = PathBuilder()
            for (particle in particleList) {
                val x = particle.x.toFloat() - vpX
                val y = particle.y.toFloat() - vpY
                val w = particle.width.toFloat()
                val h = particle.height.toFloat()
                if (particle.shape == ParticleShape.CIRCLE) {
                    builder.addOval(Rect.makeXYWH(x, y, w, h))
                } else {
                    builder.addRect(Rect.makeXYWH(x, y, w, h))
                }
            }
            val batchPath = builder.detach()
            particlePaint.color = color
            canvas.drawPath(batchPath, particlePaint)
            batchPath.close()
        }
    }

    private fun hasVisibleActiveElements(elements: ArrayList<out GameElement>, viewPort: ViewPort): Boolean {
        for (i in 0 until elements.size) {
            val element = elements[i]
            if (element.state != GameElementState.INACTIVE && element.cullingCheck(viewPort)) {
                return true
            }
        }
        return false
    }

    private fun drawMapElements(
        elements: ArrayList<out GameElement>,
        viewPort: ViewPort,
        canvas: Canvas,
        transformDirection: Boolean
    ) {
        for (i in 0 until elements.size) {
            val element = elements[i]
            if (element.state != GameElementState.INACTIVE && element.cullingCheck(viewPort)) {
                val localX = element.x - viewPort.x
                val localY = element.y - viewPort.y
                val elementImage = when (element) {
                    is Enemy -> enemyImageCache[element.type.name]
                    is Item -> itemImageCache[element.type.name]
                    else -> null
                } ?: throw IllegalStateException("No image found for element")

                val flip = transformDirection && element.nestedDirection() == Direction.LEFT
                if (flip) {
                    drawFlippedSprite(canvas, elementImage, element, localX, localY, mapElementPaint)
                } else {
                    drawSprite(canvas, elementImage, element, localX, localY, mapElementPaint)
                }
            }
        }
    }

    private fun drawFlippedSprite(
        canvas: Canvas,
        image: Image,
        element: GameElement,
        localX: Int,
        localY: Int,
        paint: Paint
    ) {
        val flippedFrame = getOrCreateFlippedFrame(image, element)
        val w = element.width.toFloat()
        val h = element.height.toFloat()
        val dstX = localX.toFloat()
        val dstY = localY.toFloat()
        canvas.drawImageRect(
            image = flippedFrame,
            srcLeft = 0f,
            srcTop = 0f,
            srcRight = w,
            srcBottom = h,
            dstLeft = dstX,
            dstTop = dstY,
            dstRight = dstX + w,
            dstBottom = dstY + h,
            samplingMode = SamplingMode.DEFAULT,
            paint = paint,
            strict = true
        )
    }

    private fun getOrCreateFlippedFrame(image: Image, element: GameElement): Image {
        val srcX = element.frameMetadata.cell.x
        val srcY = element.frameMetadata.cell.y
        val width = element.width
        val height = element.height
        val cacheKey = "${image.hashCode()}:$srcX:$srcY:$width:$height"
        val cached = flippedFrameCache[cacheKey]
        if (cached != null) {
            return cached
        }
        val surface = Surface.makeRasterN32Premul(width, height)
        val subsetCanvas = surface.canvas
        subsetCanvas.clear(0x00000000)
        subsetCanvas.save()
        try {
            subsetCanvas.translate(width.toFloat(), 0f)
            subsetCanvas.scale(-1f, 1f)
            subsetCanvas.drawImageRect(
                image = image,
                srcLeft = srcX.toFloat(),
                srcTop = srcY.toFloat(),
                srcRight = (srcX + width).toFloat(),
                srcBottom = (srcY + height).toFloat(),
                dstLeft = 0f,
                dstTop = 0f,
                dstRight = width.toFloat(),
                dstBottom = height.toFloat(),
                samplingMode = SamplingMode.DEFAULT,
                paint = null,
                strict = true
            )
        } finally {
            subsetCanvas.restore()
        }
        val flippedFrame = surface.makeImageSnapshot()
        flippedFrameCache[cacheKey] = flippedFrame
        return flippedFrame
    }

    private fun drawSprite(
        canvas: Canvas,
        image: Image,
        element: GameElement,
        localX: Int,
        localY: Int,
        paint: Paint
    ) {
        val srcX = element.frameMetadata.cell.x.toFloat()
        val srcY = element.frameMetadata.cell.y.toFloat()
        val w = element.width.toFloat()
        val h = element.height.toFloat()
        val dstX = localX.toFloat()
        val dstY = localY.toFloat()
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
