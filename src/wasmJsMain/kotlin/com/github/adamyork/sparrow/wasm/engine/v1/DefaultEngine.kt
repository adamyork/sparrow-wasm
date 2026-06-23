package com.github.adamyork.sparrow.game.engine.v1

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toArgb
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.CustomImageWrapper
import com.github.adamyork.sparrow.wasm.DrawResult
import com.github.adamyork.sparrow.wasm.common.AudioQueue
import com.github.adamyork.sparrow.wasm.common.DefaultStatusProvider
import com.github.adamyork.sparrow.wasm.data.*
import com.github.adamyork.sparrow.wasm.data.enemy.*
import com.github.adamyork.sparrow.wasm.data.item.CollectibleItem
import com.github.adamyork.sparrow.wasm.data.item.FinishItem
import com.github.adamyork.sparrow.wasm.data.item.Item
import com.github.adamyork.sparrow.wasm.data.item.ItemType
import com.github.adamyork.sparrow.wasm.data.map.GameMap
import com.github.adamyork.sparrow.wasm.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.data.player.Player
import com.github.adamyork.sparrow.wasm.data.player.PlayerJumpingState
import com.github.adamyork.sparrow.wasm.data.player.PlayerMovingState
import com.github.adamyork.sparrow.wasm.engine.Collision
import com.github.adamyork.sparrow.wasm.engine.Engine
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.engine.Physics
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.engine.data.ParticleShape
import com.github.adamyork.sparrow.wasm.engine.data.ParticleType
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.ScoreService
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.*
import kotlin.concurrent.atomics.ExperimentalAtomicApi


class DefaultEngine @AppScope @Inject constructor(
    private val physics: Physics,
    private val collision: Collision,
    private val particles: Particles,
    private val audioQueue: AudioQueue,
    private val scoreService: ScoreService,
    private val assetService: AssetService,
    private val statusProviderFactory: () -> DefaultStatusProvider
) : Engine {

    private val statusProvider: DefaultStatusProvider get() = statusProviderFactory()

    override fun setCollisionBufferedImage(customImageWrapper: CustomImageWrapper) {
        this.collision.collisionImage = customImageWrapper
        this.collision.cacheCollisionPixels()
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
        var nextX = viewPort.x
        var nextY = viewPort.y
        if (player.direction == Direction.RIGHT) {
            val adjustedX = player.x + player.width
            val viewPortRightBoundary = viewPort.x + viewPort.width
            if (adjustedX > viewPortRightBoundary) {
                //LOGGER.info("move map horizontal right")
                val diff = adjustedX - viewPortRightBoundary
                nextX =
                    (nextX + diff).coerceAtMost(collision.collisionImage.imageBitmap.width - viewPort.width)
            }
        } else {
            val viewPortLeftBoundary = viewPort.x
            if (player.x < viewPortLeftBoundary) {
                //LOGGER.info("move map horizontal left")
                val diff = viewPortLeftBoundary - player.x
                nextX = (nextX - diff).coerceAtLeast(0)
            }
        }
        val playerBottom = player.y + player.height
        val viewPortBottomBoundary = viewPort.y + viewPort.height
        if (player.y < viewPort.y) {
            // LOGGER.info("move map vertical up")
            val diff = viewPort.y - player.y
            nextY = (nextY - diff).coerceAtLeast(0)
        } else if (playerBottom > viewPortBottomBoundary) {
            // LOGGER.info("move map vertical down")
            val diff = player.y - viewPort.y
            nextY =
                (nextY + diff).coerceAtMost(collision.collisionImage.imageBitmap.height - viewPort.height)
        }
        val nextViewPort = ViewPort(nextX, nextY, viewPort.x, viewPort.y, viewPort.width, viewPort.height)
        if (nextX != viewPort.x || nextY != viewPort.y) {
            //LOGGER.info("viewport has changed $nextViewPort")
        }
        return nextViewPort
    }

    override fun manageMap(player: Player, gameMap: GameMap): GameMap {
        val managedMapItems = manageMapItems(gameMap)
        val managedMapEnemies = manageMapEnemies(gameMap, player)
        val managedCollisionParticles = physics.applyCollisionParticlePhysics(gameMap.particles)
        val managedMapItemReturnParticles = physics.applyMapItemReturnParticlePhysics(managedCollisionParticles)
        if (player.moving == PlayerMovingState.MOVING && player.jumping == PlayerJumpingState.GROUNDED) {
            val nextDustParticles = particles.createDustParticles(player)
            managedMapItemReturnParticles.addAll(nextDustParticles)
        }
        val managedDustParticles = physics.applyDustParticlePhysics(managedMapItemReturnParticles)
        val managedAllParticles = physics.applyProjectileParticlePhysics(managedDustParticles)
        var mapState = gameMap.state
        if (mapState == GameMapState.COLLECTING && scoreService.allFound()) {
            //LOGGER.info("all items found map is in completing mode")
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
        return gameMap.items.map { item ->
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
                (item as FinishItem).copy(x = itemX, y = itemY, state = nextState, frameMetadata = metadata)
            } else {
                (item as CollectibleItem).copy(
                    x = itemX,
                    y = itemY,
                    state = nextState,
                    frameMetadata = metadata
                )
            }
        }.toCollection(ArrayList())
    }

    private fun returnMapItemAfterCollision(gameMap: GameMap): ArrayList<Item> {
        val firstInactive: Item? =
            gameMap.items.firstOrNull { item -> item.type == ItemType.COLLECTABLE && item.state == GameElementState.INACTIVE }
        if (firstInactive != null) {
            val remainingItems: ArrayList<Item> =
                gameMap.items.filter { item -> item.type == ItemType.COLLECTABLE && item.id != firstInactive.id }
                    .toCollection(ArrayList())
            val reactivatedItem = (firstInactive as CollectibleItem).copy(
                state = GameElementState.ACTIVE
            )
            remainingItems.add(reactivatedItem)
            return remainingItems
        } else {
            return gameMap.items
        }
    }

    private fun manageMapEnemies(gameMap: GameMap, player: Player): ArrayList<Enemy> {
        return gameMap.enemies.map { enemy ->
            val nextState = enemy.getNextEnemyState(player)
            if (nextState != GameElementState.INACTIVE) {
                val nextPosition = enemy.getNextPosition()
                val itemX = nextPosition.x
                val itemY = nextPosition.y
                val frameMetadataWithState = (enemy as GameElement).getNextFrameMetadataWithState()
                val metadata = frameMetadataWithState.first
                val metadataState = frameMetadataWithState.second
                when (enemy.type) {
                    EnemyType.SHOOTER -> {
                        (enemy as ShooterEnemy).copy(
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
                        (enemy as RunnerEnemy).copy(
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
                        (enemy as BlockerEnemy).copy(
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
                (enemy as RunnerEnemy).copy(state = nextState)
            } else {
                enemy
            }
        }.toCollection(ArrayList())
    }

    @OptIn(ExperimentalAtomicApi::class)
    override fun draw(
        map: GameMap,
        viewPort: ViewPort,
        player: Player
    ): DrawResult {
        val imageInfo = ImageInfo.makeN32Premul(viewPort.width, viewPort.height)
        val backgroundSurface = Surface.makeRaster(imageInfo)
        val foregroundSurface = Surface.makeRaster(imageInfo)
        val backgroundCanvas = backgroundSurface.canvas
        val foregroundCanvas = foregroundSurface.canvas
        val paint = Paint().apply {
            isAntiAlias = false
            blendMode = BlendMode.SRC_OVER
        }
        paint.blendMode = BlendMode.SRC_OVER

//        if (statusProvider.lastBackgroundComposite.load().width == 1) {
//            val compositeBackgroundImage = compositeBackground(map, viewPort)
//            statusProvider.lastBackgroundComposite.store(compositeBackgroundImage)
//        }

        //if (viewPort.x != viewPort.lastX || viewPort.y != viewPort.lastY) {
        //LOGGER.info("view port has moved need to redraw background")
        //val compositeBackgroundImage = compositeBackground(map, viewPort)
        //statusProvider.lastBackgroundComposite.store(compositeBackgroundImage)
        // backgroundCanvas.drawImage(compositeBackgroundImage.makeImageSnapshot(), 0F, 0F, null)
        //} else {
        //canvas.drawImage(statusProvider.lastBackgroundComposite.load(), 0F, 0F, null)
        // }

        //drawStatusText(map, foregroundCanvas)
//        drawMapElements(
//            map.items.map { item -> item as GameElement }.toCollection(ArrayList()),
//            viewPort,
//            foregroundSurface,
//            false
//        )
//        drawMapElements(
//            map.enemies.map { item -> item as GameElement }.toCollection(ArrayList()),
//            viewPort,
//            foregroundSurface,
//            true
//        )
//        drawParticles(map, viewPort, foregroundCanvas)
        //TODO cache this
        val playerImage = Image.makeFromBitmap(player.customImageWrapper.imageBitmap.asSkiaBitmap())
        drawPlayer(player, viewPort, foregroundCanvas, playerImage)

        return DrawResult(foregroundSurface, null)
    }

    private fun compositeBackground(map: GameMap, viewPort: ViewPort): Surface {
        val imageInfo = ImageInfo.makeN32Premul(viewPort.width, viewPort.height)
        val bgCompositeImageSurface = Surface.makeRaster(imageInfo)
        val bgCompositeImageCanvas = bgCompositeImageSurface.canvas
        val farGroundSubImage = getSubImage(
            map.farGroundAsset.customImageWrapper.imageBitmap,
            map.getFarGroundX(viewPort),
            viewPort.y,
            viewPort.width,
            viewPort.height
        )
        val midGroundSubImage = getSubImage(
            map.midGroundAsset.customImageWrapper.imageBitmap,
            map.getMidGroundX(viewPort),
            viewPort.y,
            viewPort.width,
            viewPort.height
        )
        val nearFieldSubImage =
            getSubImage(
                map.nearFieldAsset.customImageWrapper.imageBitmap,
                viewPort.x,
                viewPort.y,
                viewPort.width,
                viewPort.height
            )
        val collisionSubImage =
            getSubImage(
                map.collisionAsset.imageBitmap,
                viewPort.x,
                viewPort.y,
                viewPort.width,
                viewPort.height
            )
        bgCompositeImageCanvas.drawImage(farGroundSubImage, 0F, 0F, null)
        bgCompositeImageCanvas.drawImage(midGroundSubImage, 0F, 0F, null)
        bgCompositeImageCanvas.drawImage(nearFieldSubImage, 0F, 0F, null)
        if (assetService.showCollisionMap()) {
            bgCompositeImageCanvas.drawImage(collisionSubImage, 0F, 0F, null)
        }
        return bgCompositeImageSurface
    }

    private fun drawPlayer(
        player: Player,
        viewPort: ViewPort,
        canvas: Canvas,
        image: Image
    ) {
        val localCord = viewPort.globalToLocal(player.x, player.y)
        canvas.save()
        if (player.direction == Direction.LEFT) {
            val pivotX = localCord.first + (player.width / 2f)
            val pivotY = localCord.second + (player.height / 2f)
            canvas.translate(pivotX, pivotY)
            canvas.scale(-1f, 1f)
            canvas.translate(-pivotX, -pivotY)
        }
        val paint = Paint().apply {
            isAntiAlias = true
        }
        canvas.drawImageRect(
            image = image,
            src = Rect.makeXYWH(
                player.frameMetadata.cell.x.toFloat(),
                player.frameMetadata.cell.y.toFloat(),
                player.width.toFloat(),
                player.height.toFloat()
            ),
            dst = Rect.makeXYWH(
                localCord.first.toFloat(),
                localCord.second.toFloat(),
                player.width.toFloat(),
                player.height.toFloat()
            ),
            samplingMode = SamplingMode.LINEAR,
            paint = paint,
            strict = true
        )
        canvas.restore()
    }

    private fun drawParticles(map: GameMap, viewPort: ViewPort, canvas: Canvas) {
        val surface = Surface.makeRaster(ImageInfo.makeN32Premul(viewPort.width, viewPort.height))
        val particleCanvas = surface.canvas
        map.particles.forEach { particle ->
            val localCord = viewPort.globalToLocal(particle.x, particle.y)
            particleCanvas.clear(particle.color.toArgb())
            if (particle.type == ParticleType.MAP_ITEM_RETURN) {
                val mapItemReference = map.items.first { _ -> true }
                val mapItemReferenceSubImage = getSubImage(
                    mapItemReference.customImageWrapper.imageBitmap,
                    0,
                    0,
                    mapItemReference.width,
                    mapItemReference.height
                )
                particleCanvas.drawImage(
                    mapItemReferenceSubImage,
                    localCord.first.toFloat(),
                    localCord.second.toFloat()
                )
            } else {
                if (particle.shape == ParticleShape.CIRCLE) {
                    val ovalRect = Rect.makeXYWH(
                        localCord.first.toFloat(),
                        localCord.second.toFloat(),
                        particle.width.toFloat(),
                        particle.height.toFloat()
                    )
                    val paint = Paint().apply {
                        color = Color.RED
                        mode = PaintMode.FILL
                    }
                    particleCanvas.drawOval(ovalRect, paint)
                } else {
                    val rect = Rect.makeXYWH(
                        localCord.first.toFloat(),
                        localCord.second.toFloat(),
                        particle.width.toFloat(),
                        particle.height.toFloat()
                    )
                    val paint = Paint().apply {
                        color = Color.BLACK // Replace with your color logic
                        mode = PaintMode.FILL // Ensures it fills the shape
                    }
                    particleCanvas.drawRect(rect, paint)
                }
            }

        }
        val particleImage = surface.makeImageSnapshot()
        canvas.drawImage(particleImage, 0F, 0F, null)
    }

    private fun drawStatusText(map: GameMap, canvas: Canvas) {
        val gameStatusTextImage = assetService.getTextAsset(map.state)
        val fullSkiaImage = Image.makeFromBitmap(gameStatusTextImage.customImageWrapper.imageBitmap.asSkiaBitmap())
        canvas.drawImage(fullSkiaImage, 0F, 0F, null)
    }

    private fun drawMapElements(
        elements: ArrayList<GameElement>,
        viewPort: ViewPort,
        surface: Surface,
        transformDirection: Boolean
    ) {
        elements.forEach { element ->
            val localCord = viewPort.globalToLocal(element.x, element.y)
            if (element.state != GameElementState.INACTIVE) {
                var itemSubImage = getSubImage(
                    element.customImageWrapper.imageBitmap,
                    element.frameMetadata.cell.x,
                    element.frameMetadata.cell.y,
                    element.width,
                    element.height
                )
                if (transformDirection) {
                    itemSubImage = transformDirection(itemSubImage, element.nestedDirection(), element.width)
                }
                surface.canvas.drawImage(
                    itemSubImage,
                    localCord.first.toFloat(),
                    localCord.second.toFloat(),
                    null
                )
            }
        }
    }

    private fun getSubImage(imageBitmap: ImageBitmap, x: Int, y: Int, width: Int, height: Int): Image {
        val fullSkiaImage = Image.makeFromBitmap(imageBitmap.asSkiaBitmap())
        val surface = Surface.makeRaster(ImageInfo.makeN32Premul(width, height))
        val canvas = surface.canvas
        val srcRect = Rect.makeXYWH(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        val dstRect = Rect.makeWH(width.toFloat(), height.toFloat())
        canvas.drawImageRect(
            image = fullSkiaImage,
            src = srcRect,
            dst = dstRect,
            samplingMode = SamplingMode.DEFAULT,
            paint = null,
            strict = true
        )
        val subImage = surface.makeImageSnapshot()
        fullSkiaImage.close()
        surface.close()
        return subImage
    }

    private fun transformDirection(image: Image, direction: Direction, width: Int): Image {
        val imageInfo = ImageInfo.makeN32Premul(width, image.height)
        val playerSurface = Surface.makeRaster(imageInfo)
        val canvas = playerSurface.canvas
        if (direction == Direction.LEFT) {
            canvas.save()
            canvas.translate(width.toFloat(), 0f)
            canvas.scale(-1f, 1f)
            canvas.drawImage(image, 0f, 0f)
            canvas.restore()
        } else {
            canvas.drawImage(image, 0f, 0f)
        }
        val resultImage = playerSurface.makeImageSnapshot()
        playerSurface.close()
        return resultImage
    }
}
