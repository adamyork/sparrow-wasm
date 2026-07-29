package com.github.adamyork.sparrow.android.engine

import com.github.adamyork.sparrow.platform.AppScope
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
    fun provideEngine(impl: AndroidEngine): Engine = impl

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

