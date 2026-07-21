package com.github.adamyork.sparrow.android.service

import com.github.adamyork.sparrow.android.service.v1.AndroidAssetService
import com.github.adamyork.sparrow.android.service.v1.AndroidWavService
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.service.*
import com.github.adamyork.sparrow.platform.service.v1.DefaultPhysicsSettingsService
import com.github.adamyork.sparrow.platform.service.v1.DefaultRuntimeService
import com.github.adamyork.sparrow.platform.service.v1.DefaultScoreService
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import me.tatarka.inject.annotations.Provides

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface ServiceConfig {

    @AppScope
    @Provides
    fun provideAssetService(impl: AndroidAssetService): AssetService = impl

    @AppScope
    @Provides
    fun providePhysicsSettingsService(impl: DefaultPhysicsSettingsService): PhysicsSettingsService = impl

    @AppScope
    @Provides
    fun provideScoreService(impl: DefaultScoreService): ScoreService = impl

    @AppScope
    @Provides
    fun provideWavService(impl: AndroidWavService): WavService = impl

    @AppScope
    @Provides
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }
    }

    @AppScope
    @Provides
    fun provideRuntimeService(impl: DefaultRuntimeService): RuntimeService = impl


}
