package com.github.adamyork.sparrow.wasm.gui.data

import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset

data class GameStateElements(
    var viewPort: ViewPort,
    var player: Player,
    var gameMap: GameMap,
    val playerAsset: ImageAsset,
    val mapItemCollectibleAsset: ImageAsset,
    val mapItemFinishAsset: ImageAsset,
    val mapEnemyBlockerAsset: ImageAsset,
    val mapEnemyShooterAsset: ImageAsset
)
