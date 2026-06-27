package com.github.adamyork.sparrow.wasm.common

import com.github.adamyork.sparrow.wasm.common.data.Sounds

interface AudioQueue {

    val queue: ArrayDeque<Sounds>

}