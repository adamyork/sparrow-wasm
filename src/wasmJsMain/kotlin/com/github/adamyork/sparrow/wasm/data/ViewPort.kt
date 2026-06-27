package com.github.adamyork.sparrow.wasm.data

data class ViewPort(val x: Int, val y: Int, val lastX: Int, val lastY: Int, val width: Int, val height: Int) {

    fun globalToLocal(x: Int, y: Int): Pair<Int, Int> {
        return Pair(x - this.x, y - this.y)
    }

}
