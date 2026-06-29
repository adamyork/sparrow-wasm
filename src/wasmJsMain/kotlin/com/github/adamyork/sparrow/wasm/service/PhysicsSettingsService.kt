package com.github.adamyork.sparrow.wasm.service

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface PhysicsSettingsService {
    var maxXVelocity: Double
    var maxYVelocity: Double
    var jumpDistance: Double
    var gravity: Double
    var friction: Double
    var yVelocityCoefficient: Double
    var xMovementDistance: Double
    var xAccelerationRate: Double
    var xDeaccelerationRate: Double
}