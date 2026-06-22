package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.wasm.CustomImageWrapper
import com.github.adamyork.sparrow.wasm.common.AudioQueue
import com.github.adamyork.sparrow.wasm.data.ViewPort
import com.github.adamyork.sparrow.wasm.data.map.GameMap
import com.github.adamyork.sparrow.wasm.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries

interface Collision {

    var collisionImage: CustomImageWrapper

    fun getCollisionBoundaries(player: Player): CollisionBoundaries

    fun recomputeXBoundaries(
        player: Player,
        previousBoundaries: CollisionBoundaries
    ): CollisionBoundaries

    fun checkForItemCollision(
        player: Player,
        gameMap: GameMap,
        audioQueue: AudioQueue
    ): GameMap

    fun checkForEnemyCollisionAndProximity(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: AudioQueue,
        particles: Particles
    ): Pair<Player, GameMap>

    fun checkForProjectileCollision(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: AudioQueue,
        particles: Particles
    ): Pair<Player, GameMap>

    fun cacheCollisionPixels()
}
