package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.game.engine.v1.DefaultEngine
import com.github.adamyork.sparrow.wasm.common.DefaultAudioQueue
import com.github.adamyork.sparrow.wasm.common.DefaultStatusProvider
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultCollision
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultParticles
import com.github.adamyork.sparrow.wasm.engine.v1.DefaultPhysics
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.PhysicsSettingsService
import com.github.adamyork.sparrow.wasm.service.ScoreService

class EngineConfig {

    fun physics(
        statusProvider: DefaultStatusProvider,
        physicsSettingsService: PhysicsSettingsService
    ): Physics = DefaultPhysics({ statusProvider }, physicsSettingsService)

    fun collision(physics: Physics, scoreService: ScoreService): Collision = DefaultCollision(physics, scoreService)

    fun particles(): Particles = DefaultParticles()

    fun engine(
        physics: Physics,
        collision: Collision,
        particles: Particles,
        audioQueue: DefaultAudioQueue,
        scoreService: ScoreService,
        assetService: AssetService,
        statusProvider: DefaultStatusProvider
    ): Engine {
        return DefaultEngine(physics, collision, particles, audioQueue, scoreService, assetService, { statusProvider })
    }

}
