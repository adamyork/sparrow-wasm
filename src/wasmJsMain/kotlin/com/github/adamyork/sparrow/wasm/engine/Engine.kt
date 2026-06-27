package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.wasm.data.ViewPort
import com.github.adamyork.sparrow.wasm.data.map.GameMap
import com.github.adamyork.sparrow.wasm.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.service.CustomImageWrapper

interface Engine {

    fun setCollisionBufferedImage(customImageWrapper: CustomImageWrapper)

    fun getCollisionBoundaries(player: Player): CollisionBoundaries

    fun managePlayer(player: Player, collisionBoundaries: CollisionBoundaries): Player

    fun manageViewport(player: Player, viewPort: ViewPort): ViewPort

    fun manageMap(player: Player, gameMap: GameMap): GameMap

    fun manageEnemyAndItemCollision(
        player: Player,
        map: GameMap,
        viewPort: ViewPort
    ): Pair<Player, GameMap>

    fun draw(
        map: GameMap,
        viewPort: ViewPort,
        player: Player
    ): DrawResult

}
