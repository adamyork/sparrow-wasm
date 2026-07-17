package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.wasm.common.data.enemy.DefaultMapElementFactory
import com.github.adamyork.sparrow.wasm.common.data.enemy.MapElementFactory
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultCollision
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultEngine
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultParticles
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultPhysics
import me.tatarka.inject.annotations.Provides

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface EngineConfig {

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
    fun provideMapElementFactory(impl: DefaultMapElementFactory): MapElementFactory = impl

}