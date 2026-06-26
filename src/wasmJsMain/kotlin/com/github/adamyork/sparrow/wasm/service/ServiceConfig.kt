package com.github.adamyork.sparrow.wasm.service

import com.github.adamyork.sparrow.game.engine.v1.DefaultEngine
import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.AudioQueue
import com.github.adamyork.sparrow.wasm.common.DefaultAudioQueue
import com.github.adamyork.sparrow.wasm.common.DefaultStatusProvider
import com.github.adamyork.sparrow.wasm.common.StatusProvider
import com.github.adamyork.sparrow.wasm.engine.Collision
import com.github.adamyork.sparrow.wasm.engine.Engine
import com.github.adamyork.sparrow.wasm.engine.Particles
import com.github.adamyork.sparrow.wasm.engine.Physics
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultCollision
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultParticles
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultPhysics
import com.github.adamyork.sparrow.wasm.service.v1.DefaultAssetService
import com.github.adamyork.sparrow.wasm.service.v1.DefaultPhysicsSettingsService
import com.github.adamyork.sparrow.wasm.service.v1.DefaultScoreService
import com.github.adamyork.sparrow.wasm.service.v1.DefaultWavService
import me.tatarka.inject.annotations.Provides

interface ServiceConfig {

    val randomNumberService: RandomNumberService
    val assetService: AssetService

    @AppScope
    @Provides
    fun provideRandomNumberService(impl: DefaultRandomNumberService): RandomNumberService = impl

    @AppScope
    @Provides
    fun provideAssetService(impl: DefaultAssetService): AssetService = impl

    @AppScope
    @Provides
    fun provideEngine(impl: DefaultEngine): Engine = impl

    @AppScope
    @Provides
    fun provideCollision(impl: DefaultCollision): Collision = impl

    @AppScope
    @Provides
    fun providePhysics(impl: DefaultPhysics): Physics = impl

    @AppScope
    @Provides
    fun provideParticles(impl: DefaultParticles): Particles = impl

    @AppScope
    @Provides
    fun provideStatusProvider(impl: DefaultStatusProvider): StatusProvider = impl

    @AppScope
    @Provides
    fun providePhysicsSettingsService(impl: DefaultPhysicsSettingsService): PhysicsSettingsService = impl

    @AppScope
    @Provides
    fun provideScoreService(impl: DefaultScoreService): ScoreService = impl

    @AppScope
    @Provides
    fun provideWavService(impl: DefaultWavService): WavService = impl

    @AppScope
    @Provides
    fun provideAudioQueue(impl: DefaultAudioQueue): AudioQueue = impl

}
