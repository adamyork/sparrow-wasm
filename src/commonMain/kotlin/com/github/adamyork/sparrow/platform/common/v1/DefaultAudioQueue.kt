package com.github.adamyork.sparrow.platform.common.v1

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.AudioQueue
import com.github.adamyork.sparrow.platform.common.data.Sounds
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
