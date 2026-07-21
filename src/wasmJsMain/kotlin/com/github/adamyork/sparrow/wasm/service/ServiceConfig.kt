package com.github.adamyork.sparrow.wasm.service

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.service.AssetService
import com.github.adamyork.sparrow.platform.service.PhysicsSettingsService
import com.github.adamyork.sparrow.platform.service.RuntimeService
import com.github.adamyork.sparrow.platform.service.ScoreService
import com.github.adamyork.sparrow.platform.service.WavService
import com.github.adamyork.sparrow.platform.service.v1.DefaultPhysicsSettingsService
import com.github.adamyork.sparrow.platform.service.v1.DefaultRuntimeService
import com.github.adamyork.sparrow.platform.service.v1.DefaultScoreService
import com.github.adamyork.sparrow.wasm.service.v1.WasmJsAssetService
import com.github.adamyork.sparrow.wasm.service.v1.WasmWavService
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*
import me.tatarka.inject.annotations.Provides

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface ServiceConfig {

    @AppScope
    @Provides
    fun provideAssetService(impl: WasmJsAssetService): AssetService = impl

    @AppScope
    @Provides
    fun providePhysicsSettingsService(impl: DefaultPhysicsSettingsService): PhysicsSettingsService = impl

    @AppScope
    @Provides
    fun provideScoreService(impl: DefaultScoreService): ScoreService = impl

    @AppScope
    @Provides
    fun provideWavService(impl: WasmWavService): WavService = impl

    @AppScope
    @Provides
    fun provideHttpClient(): HttpClient = HttpClient(Js) {
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
