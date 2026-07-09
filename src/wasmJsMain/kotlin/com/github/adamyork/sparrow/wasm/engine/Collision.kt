package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.wasm.common.AudioQueue
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface Collision {

    var collisionImage: ImageAndBytes

    fun getCollisionBoundaries(player: Player): CollisionBoundaries

    fun updateCollisionXBoundaries(player: Player, collisionBoundaries: CollisionBoundaries)

    fun applyAllItemCollision(player: Player, gameMap: GameMap, audioQueue: AudioQueue)

    fun applyEnemyAndProximityCollision(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: AudioQueue,
        particles: Particles
    )

    fun applyProjectileCollision(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: AudioQueue,
        particles: Particles
    )

    fun cacheCollisionPixels()
}
