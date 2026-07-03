package com.github.adamyork.sparrow.wasm.service.v1

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.charleskorn.kaml.Yaml
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.GameConfig
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.data.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.window
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.coroutineScope
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.Image
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.url.URL
import org.w3c.fetch.Response
import org.w3c.files.Blob

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultAssetService(private val httpClient: HttpClient) : AssetService {

    val mapAssetMap: HashMap<Int, ImageAsset> = HashMap()

    private val logger = KotlinLogging.logger {}

    override lateinit var gameConfig: GameConfig
    override lateinit var applicationYamlFile: String
    override var backgroundMusicBytesMap: HashMap<Int, ByteArray> = HashMap()

    private var enemyInfoMap: HashMap<Int, MapElementYamlEntry> = HashMap()
    private var itemInfoMap: HashMap<Int, MapElementYamlEntry> = HashMap()

    private var audioMap: HashMap<Sounds, String> = HashMap()
    private lateinit var backgroundAudio: String

    private val colorMap = mapOf(
        "green" to Color.Green,
        "white" to Color.White,
        "blue" to Color.Blue,
        "darkgray" to Color.DarkGray,
        "red" to Color.Red,
        "gray" to Color.Gray,
        "lightgray" to Color.LightGray,
        "yellow" to Color.Yellow,
        "magenta" to Color.Magenta,
        "black" to Color.Black
    )

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
        enemyInfoMap = gameConfig.map.enemy.positions.mapIndexed { index, pos ->
            val dim = gameConfig.map.enemy.asset[pos.ref]
                ?: throw AssetServiceReferenceException("no enemy asset for ${pos.ref}")
            index to MapElementYamlEntry(dim.path, dim.width, dim.height, pos.x, pos.y, pos.type)
        }.toMap(HashMap())
        itemInfoMap = gameConfig.map.item.positions.mapIndexed { index, pos ->
            val dim = gameConfig.map.item.asset[pos.ref]
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

    override suspend fun loadMap(id: Int, listener: LoadingProgressListener): GameMap {
        logger.info { "loading map $id" }
        val assets = listOf(
            gameConfig.map.bg,
            gameConfig.map.mg,
            gameConfig.map.fg,
            gameConfig.map.col
        ).map { path ->
            val asset = fetchImageAndBytes(path, gameConfig.map.width, gameConfig.map.height)
            listener.onTaskCompleted(path)
            asset
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
            ArrayList()
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

    override fun getTotalEnemies(): Int {
        return gameConfig.map.enemy.positions.size
    }

    override fun getEnemyPosition(id: Int): ItemPositionAndType {
        val enemy = enemyInfoMap[id] ?: throw AssetServiceReferenceException("no enemy found at $id")
        return ItemPositionAndType(enemy.x, enemy.y, enemy.type)
    }

    override fun getTotalItems(): Int {
        return gameConfig.map.item.positions.size
    }

    override fun getItemPosition(id: Int): ItemPositionAndType {
        val item = itemInfoMap[id] ?: throw AssetServiceReferenceException("no item found at $id")
        return ItemPositionAndType(item.x, item.y, item.type)
    }

    override suspend fun loadPlayer(): ImageAsset {
        return fetchImageAndBytes(
            gameConfig.player.asset.path,
            gameConfig.player.width,
            gameConfig.player.height
        )
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

    override fun getAudioPath(sound: Sounds): String {
        return audioMap[sound] ?: throw AssetServiceReferenceException("no audio path for for key $sound")
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

    private fun stringToColor(stringColor: String) =
        colorMap[stringColor.lowercase()] ?: throw AssetServiceReferenceException("unknown color provided $stringColor")

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

}
