package com.github.adamyork.sparrow.wasm.common

import com.github.adamyork.sparrow.wasm.common.data.Sounds

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface AudioQueue {

    val queue: ArrayDeque<Sounds>

}