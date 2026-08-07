package com.github.adamyork.sparrow.android.engine

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.enemy.CommonMapElementFactory
import com.github.adamyork.sparrow.platform.common.data.enemy.MapElementFactory
import com.github.adamyork.sparrow.platform.engine.Collision
import com.github.adamyork.sparrow.platform.engine.Engine
import com.github.adamyork.sparrow.platform.engine.Particles
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.CommonParticles
import com.github.adamyork.sparrow.platform.engine.CommonPhysics
import me.tatarka.inject.annotations.Provides

interface EngineConfig {

    @AppScope
    @Provides
    fun provideEngine(
        platformInterop: PlatformInterop,
        androidEngine: AndroidEngine,
        androidGpuEngine: AndroidGpuEngine
    ): Engine = if (platformInterop.isGpuEngineSupported()) androidGpuEngine else androidEngine

    @AppScope
    @Provides
    fun provideCollision(impl: AndroidTileCollision): Collision = impl

    @AppScope
    @Provides
    fun providePhysics(impl: CommonPhysics): Physics = impl

    @AppScope
    @Provides
    fun provideParticles(impl: CommonParticles): Particles = impl

    @AppScope
    @Provides
    fun provideMapElementFactory(impl: CommonMapElementFactory): MapElementFactory = impl
}
