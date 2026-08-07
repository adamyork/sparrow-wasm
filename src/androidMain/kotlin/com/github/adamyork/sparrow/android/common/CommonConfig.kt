package com.github.adamyork.sparrow.android.common

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.AudioQueue
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.CommonAudioQueue
import me.tatarka.inject.annotations.Provides

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface CommonConfig {

    val platformInterop: PlatformInterop

    @AppScope
    @Provides
    fun provideAudioQueue(impl: CommonAudioQueue): AudioQueue = impl

    @AppScope
    @Provides
    fun providePlatformInterop(impl: AndroidInterop): PlatformInterop = impl

}
