package com.github.adamyork.sparrow.platform.service.v1

import androidx.compose.ui.graphics.Color
import com.charleskorn.kaml.Yaml
import com.github.adamyork.sparrow.platform.AppProperties
import com.github.adamyork.sparrow.platform.common.data.Sounds
import com.github.adamyork.sparrow.platform.common.data.enemy.MapElementFactory
import com.github.adamyork.sparrow.platform.common.data.map.GameMap
import com.github.adamyork.sparrow.platform.common.data.map.GameMapState
import com.github.adamyork.sparrow.platform.service.AssetService
import com.github.adamyork.sparrow.platform.service.LoadingProgressListener
import com.github.adamyork.sparrow.platform.service.data.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

abstract class AbstractPlatformAssetService(
    protected val httpClient: HttpClient,
    protected val mapElementFactory: MapElementFactory
) : AssetService {

    companion object {
        private val COLOR_MAP = mapOf(
            "green" to Color.Green, "white" to Color.White, "blue" to Color.Blue,
            "darkgray" to Color.DarkGray, "red" to Color.Red, "gray" to Color.Gray,
            "lightgray" to Color.LightGray, "yellow" to Color.Yellow,
            "magenta" to Color.Magenta, "black" to Color.Black
        )
    }

    protected val logger = KotlinLogging.logger {}

    override lateinit var appProperties: AppProperties
    override var backgroundMusicBytesMap: HashMap<Int, ByteArray> = HashMap()
    override lateinit var applicationYamlFile: String

    protected val mapAssetMap: HashMap<Int, ImageAsset> = HashMap()
    protected var enemyInfoMap: HashMap<Int, MapElementYamlEntry> = HashMap()
    protected var itemInfoMap: HashMap<Int, MapElementYamlEntry> = HashMap()
    protected var audioMap: HashMap<Sounds, String> = HashMap()

    protected abstract suspend fun fetchImageAndBytes(path: String, width: Int, height: Int): ImageAsset

    override suspend fun initialize(listener: LoadingProgressListener) {
        logger.info { "initialize called loading yaml" }
        val response = httpClient.get("application.yml")
        check(response.status.isSuccess()) { "Failed to load application yml (status=${response.status})" }
        val bytes = response.body<ByteArray>()
        finishInit(bytes, listener)
    }

    protected fun finishInit(bytes: ByteArray, listener: LoadingProgressListener){
        val yamlString = bytes.decodeToString()
        listener.onTaskCompleted("app_yaml")
        appProperties = Yaml.default.decodeFromString(AppProperties.serializer(), yamlString)
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
        logger.info { "game config initialized" }
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

    override suspend fun loadMap(id: Int, listener: LoadingProgressListener): GameMap = coroutineScope {
        logger.info { "loading map $id" }
        val mapPaths = listOf(appProperties.map.bg, appProperties.map.mg, appProperties.map.fg, appProperties.map.col)

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
        GameMap(
            GameMapState.COLLECTING,
            assets[0], assets[1], assets[2], assets[3].imageAndBytes,
            assets[3].imageAndBytes.imageBitmap.width,
            assets[3].imageAndBytes.imageBitmap.height,
            ArrayList(), ArrayList(), ArrayList(),
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

    override fun getTextForGameState(gameMapState: GameMapState): TextAsset {
        return when (gameMapState) {
            GameMapState.COLLECTING -> TextAsset(
                appProperties.map.directive.initial.text,
                stringToColor(appProperties.map.directive.initial.color)
            )

            GameMapState.COMPLETING -> TextAsset(
                appProperties.map.directive.finish.text,
                stringToColor(appProperties.map.directive.finish.color)
            )

            GameMapState.COMPLETED -> TextAsset(
                appProperties.map.directive.complete.text,
                stringToColor(appProperties.map.directive.complete.color)
            )
        }
    }

    override fun showCollisionMap(): Boolean = appProperties.map.collision.visible

    protected fun stringToColor(stringColor: String) =
        COLOR_MAP[stringColor.lowercase()] ?: throw AssetServiceReferenceException("unknown color: $stringColor")
}