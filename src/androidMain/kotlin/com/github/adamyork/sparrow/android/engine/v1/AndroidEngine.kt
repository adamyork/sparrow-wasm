package com.github.adamyork.sparrow.android.engine.v1

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.graphics.asAndroidBitmap
import com.github.adamyork.sparrow.android.engine.data.AndroidPlatformImage
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.AudioQueue
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.Direction
import com.github.adamyork.sparrow.platform.common.data.ElementState
import com.github.adamyork.sparrow.platform.common.data.GameElement
import com.github.adamyork.sparrow.platform.common.data.ViewPort
import com.github.adamyork.sparrow.platform.common.data.enemy.Enemy
import com.github.adamyork.sparrow.platform.common.data.item.DefaultItem
import com.github.adamyork.sparrow.platform.common.data.item.Item
import com.github.adamyork.sparrow.platform.common.data.map.GameMap
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.engine.Collision
import com.github.adamyork.sparrow.platform.engine.Particles
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.data.DrawResult
import com.github.adamyork.sparrow.platform.engine.data.Particle
import com.github.adamyork.sparrow.platform.engine.data.ParticleShape
import com.github.adamyork.sparrow.platform.engine.data.ParticleType
import com.github.adamyork.sparrow.platform.engine.data.PlatformImage
import com.github.adamyork.sparrow.platform.engine.v1.PlatformEngine
import com.github.adamyork.sparrow.platform.service.AssetService
import com.github.adamyork.sparrow.platform.service.RuntimeService
import com.github.adamyork.sparrow.platform.service.ScoreService
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import me.tatarka.inject.annotations.Inject
import androidx.core.graphics.createBitmap

@AppScope
@Inject
class AndroidEngine(
    physics: Physics,
    collision: Collision,
    particles: Particles,
    audioQueue: AudioQueue,
    scoreService: ScoreService,
    assetService: AssetService,
    runtimeService: RuntimeService,
    platformInterop: PlatformInterop
) : PlatformEngine(
    physics,
    collision,
    particles,
    audioQueue,
    scoreService,
    assetService,
    runtimeService,
    platformInterop
) {

    override var mapItem: Item = DefaultItem()
    override var mapItemImage: PlatformImage = AndroidPlatformImage(createPlaceholderBitmap())
    override var playerImage: PlatformImage = AndroidPlatformImage(createPlaceholderBitmap())

    override val itemImageCache: HashMap<String, PlatformImage> = hashMapOf()
    override val enemyImageCache: HashMap<String, PlatformImage> = hashMapOf()
    override val flippedFrameCache: HashMap<String, PlatformImage> = hashMapOf()

    override var foregroundSurface: Any? = null

    override val mapElementPaint: Any = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    override val particlePaint: Any = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }
    override val mapItemReturnPaint: Any = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    override val playerPaintNormal: Any = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    override val playerPaintTinted: Any = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        colorFilter = PorterDuffColorFilter(0x8000FF00.toInt(), PorterDuff.Mode.SRC_ATOP)
    }

    override fun getOrCreateForegroundSurface(viewPort: ViewPort): Any {
        val current = foregroundSurface as? Bitmap
        if (current == null || current.width != viewPort.width || current.height != viewPort.height) {
            foregroundSurface = createBitmap(viewPort.width, viewPort.height)
        }
        return foregroundSurface as Bitmap
    }

    override fun initialize(gameMap: GameMap, collisionImageAndBytes: ImageAndBytes, player: Player, font: Any) {
        flippedFrameCache.clear()
        this.collision.collisionImage = collisionImageAndBytes
        this.collision.cacheCollisionPixels()

        val showItemDots = assetService.appProperties.map.itemDots.visible

        gameMap.items.forEach { item ->
            val bitmap = if (showItemDots) {
                val markedBytes = assetService.drawId(item.imageAndBytes.bytes, item.id, item.width, item.height, font)
                requireNotNull(android.graphics.BitmapFactory.decodeByteArray(markedBytes, 0, markedBytes.size)) {
                    "Failed to decode marked item image"
                }
            } else {
                item.imageAndBytes.imageBitmap.asAndroidBitmap()
            }
            itemImageCache[itemCacheKey(item)] = AndroidPlatformImage(bitmap.copy(Bitmap.Config.ARGB_8888, false))
        }

        gameMap.enemies.forEach { enemy ->
            val bitmap = if (showItemDots) {
                val markedBytes = assetService.drawId(enemy.imageAndBytes.bytes, enemy.id, enemy.width, enemy.height, font)
                requireNotNull(android.graphics.BitmapFactory.decodeByteArray(markedBytes, 0, markedBytes.size)) {
                    "Failed to decode marked enemy image"
                }
            } else {
                enemy.imageAndBytes.imageBitmap.asAndroidBitmap()
            }
            enemyImageCache[enemyCacheKey(enemy)] = AndroidPlatformImage(bitmap.copy(Bitmap.Config.ARGB_8888, false))
        }

        mapItem = gameMap.items.firstOrNull() ?: DefaultItem()
        mapItemImage = gameMap.items.firstOrNull()?.let { itemImageCache[itemCacheKey(it)] }
            ?: AndroidPlatformImage(mapItem.imageAndBytes.imageBitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false))
        playerImage = AndroidPlatformImage(player.imageAndBytes.imageBitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false))
    }

    override fun draw(map: GameMap, viewPort: ViewPort, player: Player, timestamp: Double): DrawResult {
        val foregroundBitmap = getOrCreateForegroundSurface(viewPort) as Bitmap
        val foregroundCanvas = Canvas(foregroundBitmap)
        foregroundCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        drawMapElements(map.items, viewPort, foregroundCanvas, false)
        if (hasVisibleActiveElements(map.enemies, viewPort)) {
            drawMapElements(map.enemies, viewPort, foregroundCanvas, true)
        }

        drawParticles(map, viewPort, foregroundCanvas, mapItem, mapItemImage)
        drawPlayer(player, viewPort, foregroundCanvas, playerImage)

        val foregroundSnapshot = foregroundBitmap.copy(Bitmap.Config.ARGB_8888, false)
        runtimeService.lastPaintTime = timestamp
        return DrawResult(
            foregroundImage = AndroidPlatformImage(foregroundSnapshot),
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
            nearFieldBitmap = map.nearFieldAsset.imageAndBytes.imageBitmap,
            nearFieldOffsetX = viewPort.x.toFloat(),
            nearFieldOffsetY = viewPort.y.toFloat()
        )
    }

    private fun drawPlayer(player: Player, viewPort: ViewPort, canvas: Canvas, image: PlatformImage) {
        val localX = player.x - viewPort.x
        val localY = player.y - viewPort.y
        val shouldShowTint = player.immunityTicks > 0 && (player.immunityTicks / 8) % 2 == 0
        val activePlayerPaint = if (shouldShowTint) playerPaintTinted as Paint else playerPaintNormal as Paint
        val isFlipped = player.direction == Direction.LEFT
        drawSprite(canvas, (image as AndroidPlatformImage).bitmap, player, localX, localY, activePlayerPaint, isFlipped)
    }

    private fun drawMapElements(
        elements: ArrayList<out GameElement>,
        viewPort: ViewPort,
        canvas: Canvas,
        transformDirection: Boolean
    ) {
        for (element in elements) {
            if (element.state != ElementState.INACTIVE && element.cullingCheck(viewPort)) {
                val localX = element.x - viewPort.x
                val localY = element.y - viewPort.y
                val elementImage = when (element) {
                    is Enemy -> enemyImageCache[enemyCacheKey(element)]
                    is Item -> itemImageCache[itemCacheKey(element)]
                    else -> null
                } ?: throw IllegalStateException("No image found for element")

                val isFlipped = transformDirection && element.nestedDirection() == Direction.LEFT
                drawSprite(
                    canvas,
                    (elementImage as AndroidPlatformImage).bitmap,
                    element,
                    localX,
                    localY,
                    mapElementPaint as Paint,
                    isFlipped
                )
            }
        }
    }

    private fun drawSprite(
        canvas: Canvas,
        image: Bitmap,
        element: GameElement,
        localX: Int,
        localY: Int,
        paint: Paint,
        isFlipped: Boolean
    ) {
        val dst = Rect(localX, localY, localX + element.width, localY + element.height)
        val targetBitmap = if (isFlipped) getOrCreateFlippedFrame(image, element) else image
        val src = if (isFlipped) {
            Rect(0, 0, element.width, element.height)
        } else {
            val sx = element.frameMetadata.cell.x
            val sy = element.frameMetadata.cell.y
            Rect(sx, sy, sx + element.width, sy + element.height)
        }
        canvas.drawBitmap(targetBitmap, src, dst, paint)
    }

    private fun getOrCreateFlippedFrame(image: Bitmap, element: GameElement): Bitmap {
        val srcX = element.frameMetadata.cell.x
        val srcY = element.frameMetadata.cell.y
        val width = element.width
        val height = element.height
        val cacheKey = "${image.hashCode()}:$srcX:$srcY:$width:$height"
        val cached = flippedFrameCache[cacheKey] as? AndroidPlatformImage
        if (cached != null) {
            return cached.bitmap
        }

        val boundedX = srcX.coerceIn(0, (image.width - width).coerceAtLeast(0))
        val boundedY = srcY.coerceIn(0, (image.height - height).coerceAtLeast(0))
        val frame = Bitmap.createBitmap(image, boundedX, boundedY, width, height)
        val matrix = Matrix().apply { preScale(-1f, 1f) }
        val flipped = Bitmap.createBitmap(frame, 0, 0, width, height, matrix, false)
        flippedFrameCache[cacheKey] = AndroidPlatformImage(flipped)
        return flipped
    }

    private fun drawParticles(
        map: GameMap,
        viewPort: ViewPort,
        canvas: Canvas,
        mapItem: Item?,
        mapItemImage: PlatformImage?
    ) {
        val vpX = viewPort.x.toFloat()
        val vpY = viewPort.y.toFloat()
        val particlePaint = particlePaint as Paint

        for (particle in map.particles) {
            if (!particle.cullingCheck(viewPort)) continue

            if (particle.type == ParticleType.MAP_ITEM_RETURN) {
                if (mapItem != null && mapItemImage is AndroidPlatformImage) {
                    val localX = particle.x.toFloat() - vpX
                    val localY = particle.y.toFloat() - vpY
                    val src = Rect(0, 0, mapItem.width, mapItem.height)
                    val dst = RectF(
                        localX,
                        localY,
                        localX + particle.width.toFloat(),
                        localY + particle.height.toFloat()
                    )
                    canvas.drawBitmap(mapItemImage.bitmap, src, dst, mapItemReturnPaint as Paint)
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
            particlePaint.color = particleColorArgb(particle, alphaMultiplier)

            val x = particle.x.toFloat() - vpX
            val y = particle.y.toFloat() - vpY
            if (particle.shape == ParticleShape.CIRCLE) {
                canvas.drawOval(
                    RectF(x, y, x + particle.width.toFloat(), y + particle.height.toFloat()),
                    particlePaint
                )
            } else {
                canvas.drawRect(
                    RectF(x, y, x + particle.width.toFloat(), y + particle.height.toFloat()),
                    particlePaint
                )
            }
        }
    }

    private fun particleColorArgb(particle: Particle, alphaMultiplier: Float): Int {
        val alpha = (particle.color.alpha.coerceIn(0f, 1f) * alphaMultiplier * 255f).toInt().coerceIn(0, 255)
        val red = (particle.color.red * 255f).toInt().coerceIn(0, 255)
        val green = (particle.color.green * 255f).toInt().coerceIn(0, 255)
        val blue = (particle.color.blue * 255f).toInt().coerceIn(0, 255)
        return Color.argb(alpha, red, green, blue)
    }

    private fun createPlaceholderBitmap(): Bitmap {
        return createBitmap(1, 1).apply {
            eraseColor(Color.RED)
        }
    }
}

