package com.github.adamyork.sparrow.wasm.service.v1

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.data.item.Item
import com.github.adamyork.sparrow.wasm.common.data.GameElementState
import com.github.adamyork.sparrow.wasm.common.data.item.ItemType
import com.github.adamyork.sparrow.wasm.service.ScoreService
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultScoreService : ScoreService {

    override var gameMapItem: ArrayList<Item> = ArrayList()

    override fun getTotal(): Int {
        return gameMapItem
            .filter { it.type == ItemType.COLLECTABLE }
            .size
    }

    override fun getRemaining(): Int {
        return gameMapItem
            .filter { it.type == ItemType.COLLECTABLE }
            .count { it.state == GameElementState.ACTIVE }
    }

    override fun allFound(): Boolean {
        return getRemaining() == 0
    }
}
