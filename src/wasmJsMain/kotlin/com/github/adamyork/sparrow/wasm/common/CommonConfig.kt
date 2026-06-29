package com.github.adamyork.sparrow.wasm.common

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.v1.DefaultAudioQueue
import com.github.adamyork.sparrow.wasm.common.v1.DefaultStatusProvider
import me.tatarka.inject.annotations.Provides

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface CommonConfig {

    val audioQueue: AudioQueue
    val statusProvider: StatusProvider

    @AppScope
    @Provides
    fun provideAudioQueue(impl: DefaultAudioQueue): AudioQueue = impl

    @AppScope
    @Provides
    fun provideStatusProvider(impl: DefaultStatusProvider): StatusProvider = impl
}