package com.github.adamyork.sparrow.platform.engine.data

data class CollisionEdgeInfo(
    val movementDelta: Int,
    val maxLookAhead: Int,
    val maxPossibleX: Int,
    val range: IntProgression
)
