package com.github.adamyork.sparrow.wasm.service

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