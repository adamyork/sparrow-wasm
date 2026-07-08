package com.github.adamyork.sparrow.wasm.service

import androidx.compose.ui.graphics.ImageBitmap
import com.github.adamyork.sparrow.wasm.GameConfig
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset
import com.github.adamyork.sparrow.wasm.service.data.ItemPositionAndType
import com.github.adamyork.sparrow.wasm.service.data.TextAsset
import com.github.adamyork.sparrow.wasm.service.v1.LoadingProgressListener

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface AssetService {

    var backgroundMusicBytesMap: HashMap<Int, ByteArray>
    var applicationYamlFile: String
    var gameConfig: GameConfig

    suspend fun initialize(listener: LoadingProgressListener)

    suspend fun loadBufferedImageAsync(file: String): ImageBitmap

    suspend fun loadMap(id: Int, listener: LoadingProgressListener): GameMap

    suspend fun loadPlayer(): ImageAsset

    suspend fun loadItem(id: Int): ImageAsset

    suspend fun loadEnemy(id: Int): ImageAsset

    suspend fun loadAudio(listener: LoadingProgressListener)

    fun getTotalEnemies(): Int

    fun getEnemyPosition(id: Int): ItemPositionAndType

    fun getTotalItems(): Int

    fun getItemPosition(id: Int): ItemPositionAndType

    fun getBackgroundAudio(): String

    fun getAudioPath(sound: Sounds): String

    fun getTextForGameState(gameMapState: GameMapState?): TextAsset

    fun showCollisionMap(): Boolean

}
