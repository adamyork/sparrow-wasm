package com.github.adamyork.sparrow.android.engine

import com.github.adamyork.sparrow.android.engine.data.AndroidCollision
import com.github.adamyork.sparrow.android.engine.v1.AndroidEngine
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.data.enemy.MapElementFactory
import com.github.adamyork.sparrow.platform.engine.Collision
import com.github.adamyork.sparrow.platform.engine.Engine
import com.github.adamyork.sparrow.platform.engine.Particles
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.v1.DefaultParticles
import com.github.adamyork.sparrow.platform.engine.v1.DefaultPhysics
import com.github.adamyork.sparrow.platform.common.data.enemy.DefaultMapElementFactory
import me.tatarka.inject.annotations.Provides

interface EngineConfig {

    @AppScope
    @Provides
    fun provideEngine(impl: AndroidEngine): Engine = impl

    @AppScope
    @Provides
    fun provideCollision(impl: AndroidCollision): Collision = impl

    @AppScope
    @Provides
    fun providePhysics(impl: DefaultPhysics): Physics = impl

    @AppScope
    @Provides
    fun provideParticles(impl: DefaultParticles): Particles = impl

    @AppScope
    @Provides
    fun provideMapElementFactory(impl: DefaultMapElementFactory): MapElementFactory = impl
}

