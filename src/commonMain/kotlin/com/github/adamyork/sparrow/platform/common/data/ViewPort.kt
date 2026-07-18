package com.github.adamyork.sparrow.platform.common.data

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class ViewPort(var x: Int, var y: Int, var lastX: Int, var lastY: Int, val width: Int, val height: Int) {

    fun globalToLocal(x: Int, y: Int): Pair<Int, Int> {
        return Pair(x - this.x, y - this.y)
    }

}
