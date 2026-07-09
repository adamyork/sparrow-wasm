package com.github.adamyork.sparrow.wasm.common.data.player

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
enum class PlayerJumpingState {
    GROUNDED,
    INITIAL,
    RISING,
    HEIGHT_REACHED,
    FALLING
}
