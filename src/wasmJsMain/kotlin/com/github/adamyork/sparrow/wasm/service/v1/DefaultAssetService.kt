package com.github.adamyork.sparrow.wasm.service.v1

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.charleskorn.kaml.Yaml
import com.github.adamyork.sparrow.wasm.AppProperties
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import com.github.adamyork.sparrow.wasm.common.data.enemy.MapElementFactory
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.data.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.*
import org.w3c.files.Blob

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultAssetService(
    private val httpClient: HttpClient,
    private val mapElementFactory: MapElementFactory,
    private val platformInterop: PlatformInterop
) : AssetService {

    companion object {
        private val COLOR_MAP = mapOf(
            "green" to Color.Green, "white" to Color.White, "blue" to Color.Blue,
            "darkgray" to Color.DarkGray, "red" to Color.Red, "gray" to Color.Gray,
            "lightgray" to Color.LightGray, "yellow" to Color.Yellow,
            "magenta" to Color.Magenta, "black" to Color.Black
        )
    }

    override var backgroundMusicBytesMap: HashMap<Int, ByteArray> = HashMap()

    override lateinit var applicationYamlFile: String
    override lateinit var appProperties: AppProperties

    private val logger = KotlinLogging.logger {}
    private val mapAssetMap: HashMap<Int, ImageAsset> = HashMap()
    private var enemyInfoMap: HashMap<Int, MapElementYamlEntry> = HashMap()
    private var itemInfoMap: HashMap<Int, MapElementYamlEntry> = HashMap()
    private var audioMap: HashMap<Sounds, String> = HashMap()

    private lateinit var backgroundAudio: String

    override suspend fun initialize(listener: LoadingProgressListener) {
        logger.info { "initialize called loading yaml" }
        val response = httpClient.get("application.yml")
        check(response.status.isSuccess()) { "Failed to load application yml (status=${response.status})" }
        val bytes = response.body<ByteArray>()
        val yamlString = bytes.decodeToString()
        listener.onTaskCompleted("app_yaml")
        logger.info { "yaml loaded" }
        appProperties = Yaml.default.decodeFromString(AppProperties.serializer(), yamlString)
        logger.info { "game config created" }
        enemyInfoMap = appProperties.map.enemy.positions.mapIndexed { index, pos ->
            val dim = appProperties.map.enemy.asset[pos.ref]
                ?: throw AssetServiceReferenceException("no enemy asset for ${pos.ref}")
            index to MapElementYamlEntry(dim.path, dim.width, dim.height, pos.x, pos.y, pos.type)
        }.toMap(HashMap())
        itemInfoMap = appProperties.map.item.positions.mapIndexed { index, pos ->
            val dim = appProperties.map.item.asset[pos.ref]
                ?: throw AssetServiceReferenceException("no item asset for ${pos.ref}")
            index to MapElementYamlEntry(dim.path, dim.width, dim.height, pos.x, pos.y, pos.type)
        }.toMap(HashMap())
        logger.info { "game config created" }
    }

    override suspend fun loadBufferedImageAsync(file: String): ImageBitmap {
        logger.info { "loadBufferedImageAsync called to load $file" }
        val response = httpClient.get(file)
        check(response.status.isSuccess()) { "Failed to load image from URL: $file (status=${response.status})" }
        logger.info { "image loaded" }
        val skiaImage = Image.makeFromEncoded(response.body<ByteArray>())
        try {
            logger.info { "bitmap created. complete" }
            return skiaImage.toComposeImageBitmap()
        } finally {
            skiaImage.close()
        }
    }

    override suspend fun loadSplash(): ImageAsset {
        return fetchImageAndBytes(
            appProperties.global.splash.asset.path,
            appProperties.global.splash.asset.width,
            appProperties.global.splash.asset.height
        )
    }

    override suspend fun loadEnding(): ImageAsset {
        return fetchImageAndBytes(
            appProperties.global.ending.asset.path,
            appProperties.global.ending.asset.width,
            appProperties.global.ending.asset.height
        )
    }

    override suspend fun loadMap(id: Int, listener: LoadingProgressListener): GameMap {
        logger.info { "loading map $id" }
        val mapPaths = listOf(
            appProperties.map.bg,
            appProperties.map.mg,
            appProperties.map.fg,
            appProperties.map.col
        )
        val assets = coroutineScope {
            val deferredAssets = mapPaths.map { path ->
                path to async { fetchImageAndBytes(path, appProperties.map.width, appProperties.map.height) }
            }
            deferredAssets.map { (path, deferredAsset) ->
                val asset = deferredAsset.await()
                listener.onTaskCompleted(path)
                asset
            }
        }
        mapAssetMap[id] = assets[0]
        mapAssetMap[id + 1] = assets[1]
        mapAssetMap[id + 2] = assets[2]
        logger.info { "game map created" }
        return GameMap(
            GameMapState.COLLECTING,
            assets[0],
            assets[1],
            assets[2],
            assets[3].imageAndBytes,
            assets[3].imageAndBytes.imageBitmap.width,
            assets[3].imageAndBytes.imageBitmap.height,
            ArrayList(),
            ArrayList(),
            ArrayList(),
            mapElementFactory
        )
    }

    override suspend fun loadPlayer(): ImageAsset {
        return fetchImageAndBytes(
            appProperties.player.asset.path,
            appProperties.player.width,
            appProperties.player.height
        )
    }

    override suspend fun loadItem(id: Int): ImageAsset {
        val entry = itemInfoMap[id] ?: throw AssetServiceReferenceException("Item ID $id not found")
        return fetchImageAndBytes(entry.path, entry.width, entry.height)
    }

    override suspend fun loadEnemy(id: Int): ImageAsset {
        val entry = enemyInfoMap[id] ?: throw AssetServiceReferenceException("Enemy ID $id not found")
        return fetchImageAndBytes(entry.path, entry.width, entry.height)
    }

    override suspend fun loadAudio(listener: LoadingProgressListener) = coroutineScope {
        val audioPathMap = mapOf(
            Sounds.JUMP to appProperties.audio.player.jump,
            Sounds.PLAYER_COLLISION to appProperties.audio.player.collision,
            Sounds.ENEMY_SHOOT to appProperties.audio.enemy.shoot,
            Sounds.ITEM_COLLECT to appProperties.audio.item.collect
        )

        suspend fun fetchBlob(url: String): Blob {
            val response = httpClient.get(url)
            check(response.status.isSuccess()) { "Failed to load $url(status=${response.status})" }
            val bytes = response.body<ByteArray>()
            return platformInterop.getBlobFromBytes(bytes) as Blob
        }

        val deferredAudios = audioPathMap.map { (key, path) ->
            key to async { fetchBlob(path) }
        }
        val deferredBackground = async { fetchBlob(appProperties.audio.background) }
        deferredAudios.forEach { (key, deferred) ->
            val blob = deferred.await()
            audioMap[key] = platformInterop.createAudioBlobUri(blob)
            listener.onTaskCompleted(key.name)
        }
        listener.onTaskCompleted(appProperties.audio.background)
        backgroundAudio = platformInterop.createAudioBlobUri(deferredBackground.await())
    }

    override fun getTotalEnemies(): Int = appProperties.map.enemy.positions.size

    override fun getEnemyPosition(id: Int): ItemPositionAndType {
        val enemy = enemyInfoMap[id] ?: throw AssetServiceReferenceException("no enemy found at $id")
        return ItemPositionAndType(enemy.x, enemy.y, enemy.type)
    }

    override fun getTotalItems(): Int = appProperties.map.item.positions.size

    override fun getItemPosition(id: Int): ItemPositionAndType {
        val item = itemInfoMap[id] ?: throw AssetServiceReferenceException("no item found at $id")
        return ItemPositionAndType(item.x, item.y, item.type)
    }

    override fun getBackgroundAudio(): String = backgroundAudio

    override fun getAudioPath(sound: Sounds): String {
        return audioMap[sound] ?: throw AssetServiceReferenceException("no audio path for for key $sound")
    }

    override fun getTextForGameState(gameMapState: GameMapState): TextAsset {
        return when (gameMapState) {
            GameMapState.COLLECTING -> {
                val color = stringToColor(appProperties.map.directive.initial.color)
                TextAsset(appProperties.map.directive.initial.text, color)
            }

            GameMapState.COMPLETING -> {
                val color = stringToColor(appProperties.map.directive.finish.color)
                TextAsset(appProperties.map.directive.finish.text, color)
            }

            GameMapState.COMPLETED -> {
                val color = stringToColor(appProperties.map.directive.complete.color)
                TextAsset(appProperties.map.directive.complete.text, color)
            }
        }
    }

    override fun showCollisionMap(): Boolean = appProperties.map.collision.visible

    private fun stringToColor(stringColor: String) =
        COLOR_MAP[stringColor.lowercase()]
            ?: throw AssetServiceReferenceException("unknown color provided $stringColor")

    private suspend fun fetchImageAndBytes(path: String, width: Int, height: Int): ImageAsset {
        logger.info { "fetchImageAndBytes for $path" }
        val response = httpClient.get(path)
        val bytes = response.body<ByteArray>()
        val skiaImage = Image.makeFromEncoded(bytes)
        try {
            val bitmap = skiaImage.toComposeImageBitmap()
            return ImageAsset(width, height, ImageAndBytes(bytes, bitmap))
        } finally {
            skiaImage.close()
        }
    }

    override suspend fun prepareFont(): Font {
        val response = httpClient.get("roboto_bold.ttf")
        check(response.status.isSuccess()) { "Failed to load application font (status=${response.status})" }
        val bytes = response.body<ByteArray>()
        val data = Data.makeFromBytes(bytes)
        val typeFace = FontMgr.default.makeFromData(data)
            ?: throw AssetServiceReferenceException("Could not create typeface from data")
        return Font(typeFace, 12F)
    }

    override fun drawId(
        bytes: ByteArray,
        id: Int,
        frameWidth: Int,
        frameHeight: Int,
        font: Font
    ): ByteArray {
        val image = Image.makeFromEncoded(bytes)
        val surface = Surface.makeRasterN32Premul(image.width, image.height)
        val textPaint = Paint().apply {
            color = org.jetbrains.skia.Color.BLACK
            isAntiAlias = true
        }
        try {
            val canvas = surface.canvas
            canvas.drawImage(image, 0f, 0f)
            val safeFrameWidth = if (frameWidth > 0) frameWidth else image.width
            val safeFrameHeight = if (frameHeight > 0) frameHeight else image.height
            val columns = (image.width / safeFrameWidth).coerceAtLeast(1)
            val rows = (image.height / safeFrameHeight).coerceAtLeast(1)
            val text = id.toString()
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val cellX = (column * safeFrameWidth).toFloat()
                    val cellY = (row * safeFrameHeight).toFloat()
                    val drawX = cellX + 5f
                    val drawY = cellY + 15f
                    canvas.drawString(text, drawX, drawY, font, textPaint)
                }
            }
            val data = surface.makeImageSnapshot().encodeToData() ?: return bytes
            return data.bytes
        } finally {
            image.close()
            surface.close()
            textPaint.close()
        }
    }
}
