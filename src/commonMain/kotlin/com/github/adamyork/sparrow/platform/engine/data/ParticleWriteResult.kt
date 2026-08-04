package com.github.adamyork.sparrow.platform.engine.data

data class ParticleWriteResult(
    val activeCount: Int,
    val dirtySlotRanges: List<IntRange>,
    val writtenSlots: List<Int>
)

