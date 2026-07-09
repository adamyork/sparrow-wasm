package com.github.adamyork.sparrow.wasm.service.v1

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.service.PhysicsSettingsService
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultPhysicsSettingsService : PhysicsSettingsService {

    override var maxXVelocity: Double = 7.5
    override var maxYVelocity: Double = 48.0
    override var jumpDistance: Double = 256.0
    override var gravity: Double = 7.5
    override var friction: Double = 0.90
    override var yVelocityCoefficient: Double = 0.25
    override var xMovementDistance: Double = 1.0
    override var xAccelerationRate: Double = 2.5
    override var xDeaccelerationRate: Double = 1.6
    override var projectileSpeed: Double = 10.0
    override var collisionVelocityDecay: Double = 0.85
    override var minActiveVelocity: Double = 0.5
    override var collisionKnockbackStrength: Int = 15
    override var collisionParticleSpeedCoefficient: Double = .25
    override var collisionParticleSizeMultiplier: Int = 10
    override var mapItemReturnParticleMinTravelDist: Double = 5.0
    override var mapItemReturnParticleSpeed: Double = 30.0
    override var dustParticleSpeedCoefficient: Double = .25

}
