package com.github.adamyork.sparrow.wasm.common.v1

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.AudioQueue
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultAudioQueue : AudioQueue {

    override val queue: ArrayDeque<Sounds> = ArrayDeque()

}
