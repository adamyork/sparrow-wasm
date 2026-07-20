package com.github.adamyork.sparrow.platform.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.Font
import com.github.adamyork.sparrow.platform.AppProperties
import com.github.adamyork.sparrow.platform.common.data.Sounds
import com.github.adamyork.sparrow.platform.common.data.map.GameMap
import com.github.adamyork.sparrow.platform.common.data.map.GameMapState
import com.github.adamyork.sparrow.platform.service.data.ImageAsset
import com.github.adamyork.sparrow.platform.service.data.ItemPositionAndType
import com.github.adamyork.sparrow.platform.service.data.TextAsset
/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface AssetService {

    var backgroundMusicBytesMap: HashMap<Int, ByteArray>
    var applicationYamlFile: String
    var appProperties: AppProperties

    suspend fun initialize(listener: LoadingProgressListener)

    suspend fun loadBufferedImageAsync(file: String): ImageBitmap

    suspend fun loadSplash(): ImageAsset

    suspend fun loadEnding(): ImageAsset

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

    fun getTextForGameState(gameMapState: GameMapState): TextAsset

    fun showCollisionMap(): Boolean

    suspend fun prepareFont(): Any

    fun drawId(bytes: ByteArray, id: Int, frameWidth: Int, frameHeight: Int, font: Any): ByteArray

}
