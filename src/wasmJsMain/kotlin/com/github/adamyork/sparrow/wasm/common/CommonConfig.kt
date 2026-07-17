package com.github.adamyork.sparrow.wasm.common

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.v1.DefaultAudioQueue
import me.tatarka.inject.annotations.Provides

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface CommonConfig {

    @AppScope
    @Provides
    fun provideAudioQueue(impl: DefaultAudioQueue): AudioQueue = impl

}