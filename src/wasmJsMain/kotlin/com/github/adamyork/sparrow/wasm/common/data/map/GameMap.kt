package com.github.adamyork.sparrow.wasm.common.data.map

import androidx.compose.ui.graphics.toComposeImageBitmap
import com.github.adamyork.sparrow.wasm.common.data.EmptyImage
import com.github.adamyork.sparrow.wasm.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.data.enemy.DefaultMapElementFactory
import com.github.adamyork.sparrow.wasm.common.data.enemy.Enemy
import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyType
import com.github.adamyork.sparrow.wasm.common.data.enemy.MapElementFactory
import com.github.adamyork.sparrow.wasm.common.data.item.Item
import com.github.adamyork.sparrow.wasm.common.data.item.ItemType
import com.github.adamyork.sparrow.wasm.engine.data.Particle
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */

class GameMap(
    var state: GameMapState,
    val farGroundAsset: ImageAsset,
    val midGroundAsset: ImageAsset,
    val nearFieldAsset: ImageAsset,
    val collisionAsset: ImageAndBytes,
    val width: Int,
    val height: Int,
    var items: ArrayList<Item>,
    var enemies: ArrayList<Enemy>,
    var particles: ArrayList<Particle>,
    val mapElementFactory: MapElementFactory
) {

    companion object {
        const val VIEWPORT_HORIZONTAL_FAR_PARALLAX_OFFSET: Int = 4
        const val VIEWPORT_HORIZONTAL_MID_PARALLAX_OFFSET: Int = 2
        val emptyGameMap: GameMap = GameMap(
            state = GameMapState.COLLECTING,
            farGroundAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), EmptyImage.createEmptyImage().toComposeImageBitmap())
            ),
            midGroundAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), EmptyImage.createEmptyImage().toComposeImageBitmap())
            ),
            nearFieldAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), EmptyImage.createEmptyImage().toComposeImageBitmap())
            ),
            collisionAsset = ImageAndBytes(
                byteArrayOf(),
                EmptyImage.createEmptyImage().toComposeImageBitmap()
            ),
            width = 1,
            height = 1,
            items = arrayListOf(),
            enemies = arrayListOf(),
            particles = arrayListOf(),
            mapElementFactory = DefaultMapElementFactory()
        )

    }

    fun getFarGroundX(viewPort: ViewPort): Int {
        var x = viewPort.x / VIEWPORT_HORIZONTAL_FAR_PARALLAX_OFFSET
        if (x < 0 || x > viewPort.width) {
            x = viewPort.x
        }
        return x
    }

    fun getMidGroundX(viewPort: ViewPort): Int {
        var x = viewPort.x / VIEWPORT_HORIZONTAL_MID_PARALLAX_OFFSET
        if (x < 0 || x > viewPort.width) {
            x = viewPort.x
        }
        return x
    }

    fun generateMapItems(collectibleItemAsset: ImageAsset, finishItemAsset: ImageAsset, assetService: AssetService) {
        val animationFps = assetService.appProperties.engine.fps.animation.toDouble()
        for (itemIndex in 0..<assetService.getTotalItems()) {
            val position = assetService.getItemPosition(itemIndex)
            val itemType = ItemType.from(position.type)
            val targetImageAsset = if (itemType == ItemType.FINISH) finishItemAsset else collectibleItemAsset
            items.add(
                mapElementFactory.createCollectibleItem(
                    targetImageAsset,
                    position,
                    itemType,
                    targetImageAsset.width,
                    targetImageAsset.height,
                    itemIndex,
                    animationFps
                )
            )
        }
    }

    fun generateMapEnemies(blockerEnemyAsset: ImageAsset, shooterEnemyAsset: ImageAsset, assetService: AssetService) {
        val animationFps = assetService.appProperties.engine.fps.animation.toDouble()
        for (enemyIndex in 0..<assetService.getTotalEnemies()) {
            val enemyType = EnemyType.from(assetService.getEnemyPosition(enemyIndex).type)
            val position = assetService.getEnemyPosition(enemyIndex)
            val targetImageAsset = if (enemyType == EnemyType.BLOCKER) {
                blockerEnemyAsset
            } else {
                shooterEnemyAsset
            }
            enemies.add(
                mapElementFactory.createEnemy(
                    targetImageAsset,
                    position,
                    enemyType,
                    targetImageAsset.width,
                    targetImageAsset.height,
                    enemyIndex,
                    animationFps
                )
            )
        }
    }

    fun reset(
        collectibleItemAsset: ImageAsset,
        finishItemAsset: ImageAsset,
        blockerEnemyAsset: ImageAsset,
        shooterEnemyAsset: ImageAsset,
        assetService: AssetService
    ) {
        this.state = GameMapState.COLLECTING
        this.items = ArrayList()
        this.enemies = ArrayList()
        this.particles = ArrayList()
        generateMapItems(collectibleItemAsset, finishItemAsset, assetService)
        generateMapEnemies(blockerEnemyAsset, shooterEnemyAsset, assetService)
    }

}
