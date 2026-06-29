package com.github.adamyork.sparrow.wasm.service.v1

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.service.PhysicsSettingsService
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class DefaultPhysicsSettingsService : PhysicsSettingsService {

    override var maxXVelocity: Double = 16.0
    override var maxYVelocity: Double = 48.0
    override var jumpDistance: Double = 256.0
    override var gravity: Double = 20.0
    override var friction: Double = 0.9
    override var yVelocityCoefficient: Double = 0.5
    override var xMovementDistance: Double = 1.0
    override var xAccelerationRate: Double = 1.5
    override var xDeaccelerationRate: Double = 4.0

}
