package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultCollision
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultEngine
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultParticles
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultPhysics
import me.tatarka.inject.annotations.Provides

interface EngineConfig {

    val engine: Engine
    val collision: Collision
    val physics: Physics
    val particles: Particles

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

}