package com.github.adamyork.sparrow.wasm.service

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.service.v1.DefaultAssetService
import com.github.adamyork.sparrow.wasm.service.v1.DefaultPhysicsSettingsService
import com.github.adamyork.sparrow.wasm.service.v1.DefaultScoreService
import com.github.adamyork.sparrow.wasm.service.v1.DefaultWavService
import me.tatarka.inject.annotations.Provides

interface ServiceConfig {

    val assetService: AssetService
    val physicsSettingsService: PhysicsSettingsService
    val scoreService: ScoreService
    val wavService: WavService


    @AppScope
    @Provides
    fun provideAssetService(impl: DefaultAssetService): AssetService = impl

    @AppScope
    @Provides
    fun providePhysicsSettingsService(impl: DefaultPhysicsSettingsService): PhysicsSettingsService = impl

    @AppScope
    @Provides
    fun provideScoreService(impl: DefaultScoreService): ScoreService = impl

    @AppScope
    @Provides
    fun provideWavService(impl: DefaultWavService): WavService = impl



}
