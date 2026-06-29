package com.github.adamyork.sparrow.wasm.service.v1

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.charleskorn.kaml.Yaml
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.GameConfig
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset
import com.github.adamyork.sparrow.wasm.service.data.ItemPositionAndType
import com.github.adamyork.sparrow.wasm.service.data.MapElementYamlEntry
import com.github.adamyork.sparrow.wasm.service.data.TextAsset
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.js.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.window
import kotlinx.coroutines.*
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.url.URL
import org.w3c.fetch.Response
import org.w3c.files.Blob

@AppScope
@Inject
/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class DefaultAssetService : AssetService {

    private val httpClient = HttpClient(Js)
    val mapAssetMap: HashMap<Int, ImageAsset> = HashMap()

    private val logger = KotlinLogging.logger {}

    override lateinit var gameConfig: GameConfig
    override lateinit var applicationYamlFile: String
    override lateinit var backgroundMusicBytesMap: HashMap<Int, ByteArray>

    private lateinit var enemyInfoMap: HashMap<Int, MapElementYamlEntry>
    private lateinit var itemInfoMap: HashMap<Int, MapElementYamlEntry>
    private lateinit var textAssetMap: HashMap<GameMapState, TextAsset>

    private lateinit var audioMap: HashMap<Sounds, String>
    private lateinit var backgroundAudio: String

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun initialize(listener: LoadingProgressListener) {
        logger.info { "initialize called loading yaml" }
        val response = window.fetch("application.yml").await()
        val buffer = response.arrayBuffer().await()
        val uint8Array = Int8Array(buffer)
        val bytes = ByteArray(uint8Array.length) { i -> uint8Array[i] }
        val yamlString = bytes.decodeToString()
        listener.onTaskCompleted("app_yaml")
        logger.info { "yaml loaded" }
        gameConfig = Yaml.default.decodeFromString(GameConfig.serializer(), yamlString)
        logger.info { "game config created" }
        enemyInfoMap = HashMap()
        repeat(gameConfig.map.enemy.positions.size) { index ->
            logger.info { "repeating enemy" }
            val position = gameConfig.map.enemy.positions[index]
            logger.info { index.toWord() }
            val dimensions = gameConfig.map.enemy.asset[(index + 1).toWord()]
            logger.info { dimensions?.path }
            logger.info { position.type }
            enemyInfoMap[index] = MapElementYamlEntry(
                dimensions?.path ?: "",
                dimensions?.width ?: 0,
                dimensions?.height ?: 0,
                position.x,
                position.y,
                position.type
            )
        }
        itemInfoMap = HashMap()
        repeat(gameConfig.map.item.positions.size) { index ->
            logger.info { "repeating item" }
            val position = gameConfig.map.item.positions[index]
            val dimensions = gameConfig.map.item.asset[(index + 1).toWord()]
            itemInfoMap[index] = MapElementYamlEntry(
                dimensions?.path ?: "",
                dimensions?.width ?: 0,
                dimensions?.height ?: 0,
                position.x,
                position.y,
                position.type
            )
        }
        audioMap = HashMap()
        textAssetMap = HashMap()
        logger.info { "game config created" }
    }

    fun Int.toWord(): String = when (this) {
        0 -> "zero"
        1 -> "one"
        2 -> "two"
        3 -> "three"
        4 -> "four"
        5 -> "five"
        6 -> "six"
        7 -> "seven"
        8 -> "eight"
        9 -> "nine"
        else -> this.toString()
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

    suspend fun loadAllMapImages(
        paths: List<String>,
        listener: LoadingProgressListener
    ): List<ImageBitmap> = coroutineScope {
        paths.map { path ->
            async {
                val bitmap = loadBufferedImageAsync(path)
                listener.onTaskCompleted(path)
                bitmap
            }
        }.awaitAll()
    }


    suspend fun createCustomImageWrappers(loadAllMapImages: List<ImageBitmap>): List<ImageAndBytes> =
        coroutineScope {
            loadAllMapImages.map { imageBitmap ->
                async(Dispatchers.Default) {
                    logger.info { "making a skia image from bitmap" }
                    val skiaBitmap = imageBitmap.asSkiaBitmap()
                    val image = Image.makeFromBitmap(skiaBitmap)
                    logger.info { "skia image made. getting bytes" }
                    val bytes = image.encodeToData(EncodedImageFormat.PNG)?.bytes
                        ?: throw IllegalStateException("Failed to encode ImageBitmap to ByteArray")
                    logger.info { "bytes accessed. wrapper created" }
                    ImageAndBytes(bytes, imageBitmap)
                }
            }.awaitAll()
        }

    override suspend fun loadMap(id: Int, listener: LoadingProgressListener): GameMap {
        logger.info { "loading map $id" }
        val bgAssetPaths: ArrayList<String> = ArrayList()
        bgAssetPaths.add(gameConfig.map.bg)
        bgAssetPaths.add(gameConfig.map.mg)
        bgAssetPaths.add(gameConfig.map.fg)
        bgAssetPaths.add(gameConfig.map.col)
        val allMapImages = loadAllMapImages(bgAssetPaths, listener)
        val customImages = createCustomImageWrappers(allMapImages)
        logger.info { "map $id loaded building custom images wrappers" }
        mapAssetMap[id] = ImageAsset(gameConfig.map.width, gameConfig.map.height, customImages[0])
        mapAssetMap[id + 1] = ImageAsset(gameConfig.map.width, gameConfig.map.height, customImages[1])
        mapAssetMap[id + 2] = ImageAsset(gameConfig.map.width, gameConfig.map.height, customImages[2])
        logger.info { "game map created" }
        return GameMap(
            GameMapState.COLLECTING,
            mapAssetMap[id]!!,
            mapAssetMap[id + 1]!!,
            mapAssetMap[id + 2]!!,
            customImages[3],
            customImages[3].imageBitmap.width,
            customImages[3].imageBitmap.height,
            ArrayList(),
            ArrayList(),
            ArrayList()
        )

    }

    override suspend fun loadItem(id: Int): ImageAsset {
        val path: String = itemInfoMap[id]?.path ?: ""
        val width: Int = itemInfoMap[id]?.width ?: 0
        val height: Int = itemInfoMap[id]?.height ?: 0
        val itemBitMap = loadBufferedImageAsync(path)
        val skiaBitmap = itemBitMap.asSkiaBitmap()
        val skiaImage = Image.makeFromBitmap(skiaBitmap)
        val bytes = skiaImage.encodeToData(EncodedImageFormat.PNG)?.bytes
            ?: throw IllegalStateException("Failed to encode")
        val imageAndBytes = ImageAndBytes(bytes, itemBitMap)
        return ImageAsset(width, height, imageAndBytes)
    }

    override suspend fun loadEnemy(id: Int): ImageAsset {
        val path: String = enemyInfoMap[id]?.path ?: ""
        val width: Int = enemyInfoMap[id]?.width ?: 0
        val height: Int = enemyInfoMap[id]?.height ?: 0
        val itemBitMap = loadBufferedImageAsync(path)
        val skiaBitmap = itemBitMap.asSkiaBitmap()
        val skiaImage = Image.makeFromBitmap(skiaBitmap)
        val bytes = skiaImage.encodeToData(EncodedImageFormat.PNG)?.bytes
            ?: throw IllegalStateException("Failed to encode")
        val imageAndBytes = ImageAndBytes(bytes, itemBitMap)
        return ImageAsset(width, height, imageAndBytes)
    }

    override fun getTotalEnemies(): Int {
        return gameConfig.map.enemy.positions.size
    }

    override fun getEnemyPosition(id: Int): ItemPositionAndType {
        return ItemPositionAndType(enemyInfoMap[id]?.x ?: 0, enemyInfoMap[id]?.y ?: 0, enemyInfoMap[id]?.type ?: "")
    }

    override fun getTotalItems(): Int {
        return gameConfig.map.item.positions.size
    }

    override fun getItemPosition(id: Int): ItemPositionAndType {
        return ItemPositionAndType(itemInfoMap[id]?.x ?: 0, itemInfoMap[id]?.y ?: 0, itemInfoMap[id]?.type ?: "")
    }

    override suspend fun loadPlayer(): ImageAsset {
        val itemBitMap = loadBufferedImageAsync(gameConfig.player.asset.path)
        val skiaBitmap = itemBitMap.asSkiaBitmap()
        val skiaImage = Image.makeFromBitmap(skiaBitmap)
        val bytes = skiaImage.encodeToData(EncodedImageFormat.PNG)?.bytes
            ?: throw IllegalStateException("Failed to encode")
        val imageAndBytes = ImageAndBytes(bytes, itemBitMap)
        return ImageAsset(gameConfig.player.width, gameConfig.player.height, imageAndBytes)
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun loadAudio(listener: LoadingProgressListener) = coroutineScope {
        val audioPathMap = mapOf(
            Sounds.JUMP to gameConfig.audio.player.jump,
            Sounds.PLAYER_COLLISION to gameConfig.audio.player.collision,
            Sounds.ENEMY_SHOOT to gameConfig.audio.enemy.shoot,
            Sounds.ITEM_COLLECT to gameConfig.audio.item.collect
        )

        suspend fun fetchBlob(url: String): Blob {
            val response = window.fetch(url).await().unsafeCast<Response>()
            return response.blob().await()
        }

        val deferredAudios = audioPathMap.map { (key, path) ->
            key to async { fetchBlob(path) }
        }
        val deferredBackground = async { fetchBlob(gameConfig.audio.background) }
        deferredAudios.forEach { (key, deferred) ->
            val blob = deferred.await()
            audioMap[key] = URL.createObjectURL(blob)
            listener.onTaskCompleted(key.name)
        }
        listener.onTaskCompleted(gameConfig.audio.background)
        backgroundAudio = URL.createObjectURL(deferredBackground.await())
    }

    override fun getAudio(sound: Sounds): String {
        return audioMap[sound]!!
    }

    override fun getTextForGameState(gameMapState: GameMapState): TextAsset {
        when (gameMapState) {
            GameMapState.COLLECTING -> {
                val color = stringToColor(gameConfig.map.directive.initial.color)
                return TextAsset(gameConfig.map.directive.initial.text, color)
            }

            GameMapState.COMPLETING -> {
                val color = stringToColor(gameConfig.map.directive.finish.color)
                return TextAsset(gameConfig.map.directive.finish.text, color)
            }

            else -> {
                val color = stringToColor(gameConfig.map.directive.complete.color)
                return TextAsset(gameConfig.map.directive.complete.text, color)
            }
        }
    }

    override fun getBackgroundAudio(): String {
        return backgroundAudio
    }

    override fun showCollisionMap(): Boolean {
        return gameConfig.map.collision.visible
    }

    private fun stringToColor(stringColor: String): Color {
        return when (stringColor.lowercase()) {
            "green" -> Color.Green
            "white" -> Color.White
            "blue" -> Color.Blue
            "darkgray" -> Color.DarkGray
            "red" -> Color.Red
            "gray" -> Color.Gray
            "lightgray" -> Color.LightGray
            "yellow" -> Color.Yellow
            "magenta" -> Color.Magenta
            else -> Color.Black
        }
    }

}
