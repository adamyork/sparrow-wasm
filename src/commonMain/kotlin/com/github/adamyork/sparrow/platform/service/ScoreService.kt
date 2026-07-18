package com.github.adamyork.sparrow.platform.service

import com.github.adamyork.sparrow.platform.common.data.item.Item

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface ScoreService {

    var gameMapItem: ArrayList<Item>

    fun getTotal(): Int

    fun getRemaining(): Int

    fun allFound(): Boolean

}
