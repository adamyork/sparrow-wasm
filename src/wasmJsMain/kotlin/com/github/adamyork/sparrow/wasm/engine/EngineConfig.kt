package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.data.enemy.DefaultMapElementFactory
import com.github.adamyork.sparrow.platform.common.data.enemy.MapElementFactory
import com.github.adamyork.sparrow.platform.engine.Collision
import com.github.adamyork.sparrow.platform.engine.Engine
import com.github.adamyork.sparrow.platform.engine.Particles
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.v1.DefaultParticles
import com.github.adamyork.sparrow.platform.engine.v1.DefaultPhysics
import com.github.adamyork.sparrow.wasm.engine.v2.WasmJsTileCollision
import com.github.adamyork.sparrow.wasm.engine.v1.WasmJsEngine
import me.tatarka.inject.annotations.Provides

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface EngineConfig {

    @AppScope
    @Provides
    fun provideEngine(impl: WasmJsEngine): Engine = impl

    @AppScope
    @Provides
    fun provideCollision(impl: WasmJsTileCollision): Collision = impl

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