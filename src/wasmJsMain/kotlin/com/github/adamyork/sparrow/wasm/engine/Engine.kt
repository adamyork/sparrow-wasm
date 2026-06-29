package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.wasm.engine.data.DrawResult
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface Engine {

    fun setCollisionBufferedImage(imageAndBytes: ImageAndBytes)

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
