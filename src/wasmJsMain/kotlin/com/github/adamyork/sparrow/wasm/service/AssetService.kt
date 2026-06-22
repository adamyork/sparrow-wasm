package com.github.adamyork.sparrow.wasm.service

import androidx.compose.ui.graphics.ImageBitmap
import com.github.adamyork.sparrow.wasm.GameConfig
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import com.github.adamyork.sparrow.wasm.data.map.GameMap
import com.github.adamyork.sparrow.wasm.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset
import com.github.adamyork.sparrow.wasm.service.data.ItemPositionAndType
import com.github.adamyork.sparrow.wasm.service.data.TextAsset

interface AssetService {

    var backgroundMusicBytesMap: HashMap<Int, ByteArray>
    var applicationYamlFile: String
    var gameConfig: GameConfig

     suspend fun initialize()

    suspend fun loadBufferedImageAsync(file: String): ImageBitmap

    suspend fun loadMap(id: Int): GameMap

    suspend fun  loadItem(id: Int): ImageAsset

    suspend fun loadEnemy(id: Int): ImageAsset

    fun getTotalEnemies(): Int

    fun getEnemyPosition(id: Int): ItemPositionAndType

    fun getTotalItems(): Int

    fun getItemPosition(id: Int): ItemPositionAndType

    suspend fun loadPlayer(): ImageAsset

    suspend fun getSoundStream(sound: Sounds): ByteArray

    fun getTextAsset(gameMapState: GameMapState): TextAsset

    fun showCollisionMap(): Boolean

}
