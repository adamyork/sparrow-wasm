package com.github.adamyork.sparrow.platform.engine

import com.github.adamyork.sparrow.platform.common.data.ControlAction
import com.github.adamyork.sparrow.platform.common.data.ViewPort
import com.github.adamyork.sparrow.platform.common.data.map.GameMap
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.platform.engine.data.DrawResult
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import com.github.adamyork.sparrow.platform.service.data.ImageAsset
import org.jetbrains.skia.Font

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface Engine {

    //TODO Interop
    fun initialize(gameMap: GameMap, collisionImageAndBytes: ImageAndBytes, player: Player, font: Font)

    fun getCollisionBoundaries(player: Player): CollisionBoundaries

    fun managePlayer(player: Player, collisionBoundaries: CollisionBoundaries)

    fun manageViewport(player: Player, viewPort: ViewPort)

    fun manageMap(player: Player, gameMap: GameMap, viewPort: ViewPort)

    fun manageEnemyAndItemCollision(player: Player, map: GameMap, viewPort: ViewPort)

    fun draw(map: GameMap, viewPort: ViewPort, player: Player, timestamp: Double): DrawResult

    fun createDefaultPlayer(playerAsset: ImageAsset): Player

    fun startInput(controlAction: ControlAction, player: Player)

    fun stopInput(controlAction: ControlAction, player: Player)

}
