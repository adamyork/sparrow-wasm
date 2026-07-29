package com.github.adamyork.sparrow.platform.common

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.data.Sounds
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class CommonAudioQueue : AudioQueue {

    override val queue: ArrayDeque<Sounds> = ArrayDeque()

}
