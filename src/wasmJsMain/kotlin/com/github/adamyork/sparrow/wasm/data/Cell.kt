package com.github.adamyork.sparrow.wasm.data

class Cell {

    val x: Int
    val y: Int

    constructor(row: Int, column: Int, width: Int, height: Int) {
        x = (column - 1) * width
        y = (row - 1) * height
    }
}
