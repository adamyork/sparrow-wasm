package com.github.adamyork.sparrow.android.service

import android.media.MediaPlayer
import com.github.adamyork.sparrow.platform.common.data.Sounds

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class SfxSlot(
    val player: MediaPlayer,
    var loadedSound: Sounds? = null
)
