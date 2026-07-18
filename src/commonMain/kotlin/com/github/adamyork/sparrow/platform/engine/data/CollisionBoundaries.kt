package com.github.adamyork.sparrow.platform.engine.data

import kotlin.math.max
import kotlin.math.min

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class CollisionBoundaries(var left: Int, var right: Int, var top: Int, var bottom: Int) {

    val minX: Int get() = min(left, right)
    val maxX: Int get() = max(left, right)
    val minY: Int get() = min(top, bottom)
    val maxY: Int get() = max(top, bottom)

}
