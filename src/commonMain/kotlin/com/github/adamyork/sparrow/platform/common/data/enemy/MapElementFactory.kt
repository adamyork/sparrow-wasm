package com.github.adamyork.sparrow.platform.common.data.enemy

import com.github.adamyork.sparrow.platform.common.data.item.ItemType
import com.github.adamyork.sparrow.platform.service.data.ImageAsset
import com.github.adamyork.sparrow.platform.service.data.ItemPositionAndType
import com.github.adamyork.sparrow.platform.common.data.item.CollectibleItem

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface MapElementFactory {

    fun createCollectibleItem(
        imageAsset: ImageAsset,
        position: ItemPositionAndType,
        itemType: ItemType,
        width: Int,
        height: Int,
        id: Int,
        animationFps: Double
    ): CollectibleItem

    fun createEnemy(
        imageAsset: ImageAsset,
        position: ItemPositionAndType,
        enemyType: EnemyType,
        width: Int,
        height: Int,
        id: Int,
        animationFps: Double
    ): Enemy

}
