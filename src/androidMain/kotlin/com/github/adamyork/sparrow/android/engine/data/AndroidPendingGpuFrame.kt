package com.github.adamyork.sparrow.android.engine.data

internal data class AndroidPendingGpuFrame(
    val deltaTimeSeconds: Float,
    val gravity: Float,
    val projectileSpeed: Float,
    val mapItemReturnSpeed: Float,
    val mapItemReturnMinTravelDist: Float,
    val playerX: Float,
    val playerY: Float,
    val playerWidth: Float,
    val playerHeight: Float
)

