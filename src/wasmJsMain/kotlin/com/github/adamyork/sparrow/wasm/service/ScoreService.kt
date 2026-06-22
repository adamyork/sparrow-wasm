package com.github.adamyork.sparrow.wasm.service

import com.github.adamyork.sparrow.wasm.data.item.Item

interface ScoreService {

    var gameMapItem: ArrayList<Item>

    fun getTotal(): Int

    fun getRemaining(): Int

    fun allFound(): Boolean
}
