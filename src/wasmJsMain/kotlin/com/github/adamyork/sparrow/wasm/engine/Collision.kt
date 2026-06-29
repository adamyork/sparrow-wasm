package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.wasm.common.v1.DefaultAudioQueue
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes

interface Collision {

    var collisionImage: ImageAndBytes

    fun getCollisionBoundaries(player: Player): CollisionBoundaries

    fun recomputeXBoundaries(
        player: Player,
        previousBoundaries: CollisionBoundaries
    ): CollisionBoundaries

    fun checkForItemCollision(
        player: Player,
        gameMap: GameMap,
        audioQueue: DefaultAudioQueue
    ): GameMap

    fun checkForEnemyCollisionAndProximity(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: DefaultAudioQueue,
        particles: Particles
    ): Pair<Player, GameMap>

    fun checkForProjectileCollision(
        player: Player,
        gameMap: GameMap,
        viewPort: ViewPort,
        audioQueue: DefaultAudioQueue,
        particles: Particles
    ): Pair<Player, GameMap>

    fun cacheCollisionPixels()
}
