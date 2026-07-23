package com.github.adamyork.sparrow.platform.common.data.map

import androidx.compose.ui.graphics.ImageBitmap
import com.github.adamyork.sparrow.platform.common.data.ViewPort
import com.github.adamyork.sparrow.platform.common.data.enemy.Enemy
import com.github.adamyork.sparrow.platform.common.data.enemy.EnemyType
import com.github.adamyork.sparrow.platform.common.data.enemy.MapElementFactory
import com.github.adamyork.sparrow.platform.common.data.item.Item
import com.github.adamyork.sparrow.platform.common.data.item.ItemType
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import com.github.adamyork.sparrow.platform.service.data.ImageAsset
import com.github.adamyork.sparrow.platform.service.data.ItemPositionAndType
import com.github.adamyork.sparrow.platform.engine.data.Particle
import com.github.adamyork.sparrow.platform.service.AssetService
import io.github.oshai.kotlinlogging.KotlinLogging

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

    private val logger = KotlinLogging.logger {}

    companion object {
        const val VIEWPORT_HORIZONTAL_FAR_PARALLAX_OFFSET: Int = 4
        const val VIEWPORT_HORIZONTAL_MID_PARALLAX_OFFSET: Int = 2
        private val EMPTY_MAP_ELEMENT_FACTORY = object : MapElementFactory {
            override fun createCollectibleItem(
                imageAsset: ImageAsset,
                position: ItemPositionAndType,
                itemType: ItemType,
                width: Int,
                height: Int,
                id: Int,
                animationFps: Double
            ) = throw UnsupportedOperationException("Empty GameMap does not create map elements")

            override fun createEnemy(
                imageAsset: ImageAsset,
                position: ItemPositionAndType,
                enemyType: EnemyType,
                width: Int,
                height: Int,
                id: Int,
                animationFps: Double
            ) = throw UnsupportedOperationException("Empty GameMap does not create map elements")
        }
        val emptyGameMap: GameMap = GameMap(
            state = GameMapState.COLLECTING,
            farGroundAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), ImageBitmap(1, 1))
            ),
            midGroundAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), ImageBitmap(1, 1))
            ),
            nearFieldAsset = ImageAsset(
                1,
                1,
                ImageAndBytes(byteArrayOf(), ImageBitmap(1, 1))
            ),
            collisionAsset = ImageAndBytes(
                byteArrayOf(),
                ImageBitmap(1, 1)
            ),
            width = 1,
            height = 1,
            items = arrayListOf(),
            enemies = arrayListOf(),
            particles = arrayListOf(),
            mapElementFactory = EMPTY_MAP_ELEMENT_FACTORY
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
            val createdItem =
                mapElementFactory.createCollectibleItem(
                    targetImageAsset,
                    position,
                    itemType,
                    targetImageAsset.width,
                    targetImageAsset.height,
                    itemIndex,
                    animationFps
                )
            items.add(createdItem)
            logger.debug {
                "Created map item id=${createdItem.id} type=${createdItem.type} state=${createdItem.state} x=${createdItem.x} y=${createdItem.y}"
            }
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
            val createdEnemy =
                mapElementFactory.createEnemy(
                    targetImageAsset,
                    position,
                    enemyType,
                    targetImageAsset.width,
                    targetImageAsset.height,
                    enemyIndex,
                    animationFps
                )
            enemies.add(createdEnemy)
            logger.debug {
                "Created enemy id=${createdEnemy.id} type=${createdEnemy.type} state=${createdEnemy.state} x=${createdEnemy.x} y=${createdEnemy.y}"
            }
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
