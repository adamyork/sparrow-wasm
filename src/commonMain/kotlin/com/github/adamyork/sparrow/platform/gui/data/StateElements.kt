package com.github.adamyork.sparrow.platform.gui.data

import androidx.compose.ui.graphics.ImageBitmap
import com.github.adamyork.sparrow.platform.common.data.ViewPort
import com.github.adamyork.sparrow.platform.common.data.map.GameMap
import com.github.adamyork.sparrow.platform.common.data.player.Player
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
            splashImage = ImageBitmap(1, 1),
            endingImage = ImageBitmap(1, 1),
            playerAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), ImageBitmap(1, 1))
            ),
            mapItemCollectibleAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), ImageBitmap(1, 1))
            ),
            mapItemFinishAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), ImageBitmap(1, 1))
            ),
            mapEnemyBlockerAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), ImageBitmap(1, 1))
            ),
            mapEnemyShooterAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), ImageBitmap(1, 1))
            ),
            scoreLabel = "Score: --",
            totalLabel = "Total: --",
            remainingLabel = "Remaining: --"
        )
    }
}
