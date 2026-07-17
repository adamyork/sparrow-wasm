package com.github.adamyork.sparrow.wasm.gui.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.github.adamyork.sparrow.wasm.common.data.EmptyImage
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.map.GameMap
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import com.github.adamyork.sparrow.platform.service.data.ImageAsset

data class StateElements(
    var viewPort: ViewPort,
    var player: Player,
    var gameMap: GameMap,
    var splashImage: ImageBitmap,
    var endingImage: ImageBitmap,
    var playerAsset: ImageAsset,
    var mapItemCollectibleAsset: ImageAsset,
    var mapItemFinishAsset: ImageAsset,
    var mapEnemyBlockerAsset: ImageAsset,
    var mapEnemyShooterAsset: ImageAsset,
    var scoreLabel: String,
    var totalLabel: String,
    var remainingLabel: String
) {
    companion object {
        val emptyStateElements: StateElements = StateElements(
            viewPort = ViewPort(0, 0, 0, 0, 1, 1),
            player = Player.emptyPlayer,
            gameMap = GameMap.emptyGameMap,
            splashImage = EmptyImage.createEmptyImage().toComposeImageBitmap(),
            endingImage = EmptyImage.createEmptyImage().toComposeImageBitmap(),
            playerAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), EmptyImage.createEmptyImage().toComposeImageBitmap())
            ),
            mapItemCollectibleAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), EmptyImage.createEmptyImage().toComposeImageBitmap())
            ),
            mapItemFinishAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), EmptyImage.createEmptyImage().toComposeImageBitmap())
            ),
            mapEnemyBlockerAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), EmptyImage.createEmptyImage().toComposeImageBitmap())
            ),
            mapEnemyShooterAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), EmptyImage.createEmptyImage().toComposeImageBitmap())
            ),
            scoreLabel = "Score: --",
            totalLabel = "Total: --",
            remainingLabel = "Remaining: --"
        )
    }
}
