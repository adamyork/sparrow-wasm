package com.github.adamyork.sparrow.platform.common

import com.github.adamyork.sparrow.platform.common.data.Sounds

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface AudioQueue {

    val queue: ArrayDeque<Sounds>

}