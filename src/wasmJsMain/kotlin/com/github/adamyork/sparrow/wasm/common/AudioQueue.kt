package com.github.adamyork.sparrow.wasm.common

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class AudioQueue {

    val queue: ArrayDeque<Sounds> = ArrayDeque()

}
