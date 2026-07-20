package com.github.adamyork.sparrow.wasm.engine.v1

import androidx.compose.ui.graphics.asSkiaBitmap
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
import com.github.adamyork.sparrow.platform.engine.data.*
import com.github.adamyork.sparrow.platform.engine.v1.PlatformEngine
import com.github.adamyork.sparrow.platform.service.AssetService
import com.github.adamyork.sparrow.platform.service.RuntimeService
import com.github.adamyork.sparrow.platform.service.ScoreService
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import com.github.adamyork.sparrow.wasm.engine.data.WasmJsPlatformImage
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.*

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class WasmJsEngine(
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

    private val logger = KotlinLogging.logger {}

    override var mapItem: Item = DefaultItem()
    override var mapItemImage: PlatformImage = WasmJsPlatformImage(
        Image.makeRaster(
            ImageInfo.makeN32(1, 1, ColorAlphaType.PREMUL),
            byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte()), // ARGB or RGBA depending on N32
            4
        )
    )
    override var playerImage: PlatformImage = WasmJsPlatformImage(
        Image.makeRaster(
            ImageInfo.makeN32(1, 1, ColorAlphaType.PREMUL),
            byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte()), // ARGB or RGBA depending on N32
            4
        )
    )

    override val itemImageCache: HashMap<String, PlatformImage> = hashMapOf()
    override val enemyImageCache: HashMap<String, PlatformImage> = hashMapOf()
    override val flippedFrameCache: HashMap<String, PlatformImage> = hashMapOf()

    override var foregroundSurface: Any? = null

    override val mapElementPaint = Paint().apply { isAntiAlias = true }
    override val particlePaint = Paint().apply { isAntiAlias = false; mode = PaintMode.FILL }
    override val mapItemReturnPaint = Paint().apply { isAntiAlias = true }
    override val playerPaintNormal = Paint().apply { isAntiAlias = true }
    override val playerPaintTinted = Paint().apply {
        isAntiAlias = true
        colorFilter = ColorFilter.makeBlend(0x8000FF00.toInt(), BlendMode.SRC_ATOP)
    }

    override fun getOrCreateForegroundSurface(viewPort: ViewPort): Surface =
        (foregroundSurface as Surface?) ?: Surface.makeRaster(ImageInfo.makeN32Premul(viewPort.width, viewPort.height))
            .also {
                foregroundSurface = it
            }

    override fun initialize(gameMap: GameMap, collisionImageAndBytes: ImageAndBytes, player: Player, font: Any) {
        flippedFrameCache.values.forEach { (it as WasmJsPlatformImage).image.close() }
        flippedFrameCache.clear()
        this.collision.collisionImage = collisionImageAndBytes
        this.collision.cacheCollisionPixels()
        val showItemDots = assetService.appProperties.map.itemDots.visible
        gameMap.items.forEach { item ->
            val image = if (showItemDots) {
                val markedBytes =
                    assetService.drawId(item.imageAndBytes.bytes, item.id, item.width, item.height, font)
                Image.makeFromEncoded(markedBytes)
            } else {
                Image.makeFromBitmap(item.imageAndBytes.imageBitmap.asSkiaBitmap())
            }
            itemImageCache[itemCacheKey(item)] = WasmJsPlatformImage(image)
        }
        gameMap.enemies.forEach { enemy ->
            val image = if (showItemDots) {
                val markedBytes =
                    assetService.drawId(enemy.imageAndBytes.bytes, enemy.id, enemy.width, enemy.height, font)
                Image.makeFromEncoded(markedBytes)
            } else {
                Image.makeFromBitmap(enemy.imageAndBytes.imageBitmap.asSkiaBitmap())
            }
            enemyImageCache[enemyCacheKey(enemy)] = WasmJsPlatformImage(image)
        }
        mapItem = gameMap.items.firstOrNull() ?: DefaultItem()
        mapItemImage = gameMap.items.firstOrNull()?.let { itemImageCache[itemCacheKey(it)] }
            ?: WasmJsPlatformImage(Image.makeFromBitmap(mapItem.imageAndBytes.imageBitmap.asSkiaBitmap()))
        playerImage = WasmJsPlatformImage(Image.makeFromBitmap(player.imageAndBytes.imageBitmap.asSkiaBitmap()))
    }

    override fun draw(map: GameMap, viewPort: ViewPort, player: Player, timestamp: Double): DrawResult {
        val foregroundSurface = getOrCreateForegroundSurface(viewPort)
        val foregroundCanvas = foregroundSurface.canvas
        foregroundCanvas.clear(0x00000000)

        drawMapElements(map.items, viewPort, foregroundCanvas, false)

        if (hasVisibleActiveElements(map.enemies, viewPort)) {
            drawMapElements(map.enemies, viewPort, foregroundCanvas, true)
        }

        drawParticles(map, viewPort, foregroundCanvas, mapItem, mapItemImage)
        drawPlayer(player, viewPort, foregroundCanvas, playerImage)

        val foregroundImage = foregroundSurface.makeImageSnapshot()
        runtimeService.lastPaintTime = timestamp
        return DrawResult(
            foregroundImage = WasmJsPlatformImage(foregroundImage),
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
        val activePlayerPaint = if (shouldShowTint) playerPaintTinted else playerPaintNormal
        val isFlipped = player.direction == Direction.LEFT

        drawSprite(canvas, (image as WasmJsPlatformImage).image, player, localX, localY, activePlayerPaint, isFlipped)
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
                    (elementImage as WasmJsPlatformImage).image,
                    element,
                    localX,
                    localY,
                    mapElementPaint,
                    isFlipped
                )
            }
        }
    }

    private fun drawSprite(
        canvas: Canvas,
        image: Image,
        element: GameElement,
        localX: Int,
        localY: Int,
        paint: Paint,
        isFlipped: Boolean
    ) {
        val (w, h) = element.width.toFloat() to element.height.toFloat()
        val (dstX, dstY) = localX.toFloat() to localY.toFloat()

        val targetImage = if (isFlipped) getOrCreateFlippedFrame(image, element) else image
        val srcX = if (isFlipped) 0f else element.frameMetadata.cell.x.toFloat()
        val srcY = if (isFlipped) 0f else element.frameMetadata.cell.y.toFloat()

        canvas.drawImageRect(
            image = targetImage,
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

    private fun getOrCreateFlippedFrame(image: Image, element: GameElement): Image {
        val srcX = element.frameMetadata.cell.x
        val srcY = element.frameMetadata.cell.y
        val width = element.width
        val height = element.height
        val cacheKey = "${image.hashCode()}:$srcX:$srcY:$width:$height"
        return flippedFrameCache[cacheKey]?.let { (it as WasmJsPlatformImage).image } ?: run {
            val surface = Surface.makeRasterN32Premul(width, height)
            surface.canvas.apply {
                clear(0x00000000)
                save()
                translate(width.toFloat(), 0f)
                scale(-1f, 1f)
                drawImageRect(
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
                restore()
            }
            val flippedFrame = surface.makeImageSnapshot()
            flippedFrameCache[cacheKey] = WasmJsPlatformImage(flippedFrame)
            flippedFrame
        }
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
        val groups = mutableMapOf<Int, MutableList<Particle>>()

        for (particle in map.particles) {
            if (!particle.cullingCheck(viewPort)) continue
            if (particle.type == ParticleType.MAP_ITEM_RETURN) {
                if (mapItem != null && mapItemImage != null) {
                    val localX = particle.x.toFloat() - vpX
                    val localY = particle.y.toFloat() - vpY
                    canvas.drawImageRect(
                        image = (mapItemImage as WasmJsPlatformImage).image,
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
                if (particle.shape == ParticleShape.CIRCLE) {
                    builder.addOval(Rect.makeXYWH(x, y, particle.width.toFloat(), particle.height.toFloat()))
                } else {
                    builder.addRect(Rect.makeXYWH(x, y, particle.width.toFloat(), particle.height.toFloat()))
                }
            }
            val batchPath = builder.detach()
            particlePaint.color = color
            canvas.drawPath(batchPath, particlePaint)
            batchPath.close()
        }
    }

}
