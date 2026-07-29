package com.github.adamyork.sparrow.platform.service

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.data.ElementState
import com.github.adamyork.sparrow.platform.common.data.item.Item
import com.github.adamyork.sparrow.platform.common.data.item.ItemType
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class CommonScoreService : ScoreService {

    private var internalGameMapItem: ArrayList<Item> = ArrayList()
    private var cachedTotal: Int = 0
    override var gameMapItem: ArrayList<Item>
        get() = internalGameMapItem
        set(value) {
            internalGameMapItem = value
            cachedTotal = internalGameMapItem.count { it.type == ItemType.COLLECTABLE }
        }

    override fun getTotal(): Int = cachedTotal

    override fun getRemaining(): Int {
        return internalGameMapItem.count { it.type == ItemType.COLLECTABLE && it.state == ElementState.ACTIVE }
    }

    override fun allFound(): Boolean = getRemaining() == 0
}
