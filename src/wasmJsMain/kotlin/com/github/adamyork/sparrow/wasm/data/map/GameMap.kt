package com.github.adamyork.sparrow.wasm.data.map

import com.github.adamyork.sparrow.wasm.CustomImageWrapper
import com.github.adamyork.sparrow.wasm.data.*
import com.github.adamyork.sparrow.wasm.data.enemy.*
import com.github.adamyork.sparrow.wasm.data.item.CollectibleItem
import com.github.adamyork.sparrow.wasm.data.item.FinishItem
import com.github.adamyork.sparrow.wasm.data.item.Item
import com.github.adamyork.sparrow.wasm.data.item.ItemType
import com.github.adamyork.sparrow.wasm.engine.data.Particle
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset

data class GameMap(
    var state: GameMapState,
    val farGroundAsset: ImageAsset,
    val midGroundAsset: ImageAsset,
    val nearFieldAsset: ImageAsset,
    val collisionAsset: CustomImageWrapper,
    val width: Int,
    val height: Int,
    var items: ArrayList<Item>,
    var enemies: ArrayList<Enemy>,
    var particles: ArrayList<Particle>
) {

    companion object {
        const val VIEWPORT_HORIZONTAL_FAR_PARALLAX_OFFSET: Int = 4
        const val VIEWPORT_HORIZONTAL_MID_PARALLAX_OFFSET: Int = 2
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
        for (i in 0..<assetService.getTotalItems()) {
            val itemType = ItemType.from(assetService.getItemPosition(i).type)
            if (itemType == ItemType.FINISH) {
                items.add(
                    FinishItem(
                        finishItemAsset.width,
                        finishItemAsset.height,
                        assetService.getItemPosition(i).x,
                        assetService.getItemPosition(i).y,
                        ItemType.FINISH,
                        GameElementState.INACTIVE,
                        finishItemAsset.customImageWrapper,
                        FrameMetadata(1, Cell(1, 1, width, height)),
                        i
                    )
                )
            } else {
                items.add(
                    CollectibleItem(
                        collectibleItemAsset.width,
                        collectibleItemAsset.height,
                        assetService.getItemPosition(i).x,
                        assetService.getItemPosition(i).y,
                        ItemType.COLLECTABLE,
                        GameElementState.ACTIVE,
                        collectibleItemAsset.customImageWrapper,
                        FrameMetadata(1, Cell(1, 1, width, height)),
                        i
                    )
                )
            }
        }
    }

    fun generateMapEnemies(blockerEnemyAsset: ImageAsset, shooterEnemyAsset: ImageAsset, assetService: AssetService) {
        for (i in 0..<assetService.getTotalEnemies()) {
            val itemType = EnemyType.from(assetService.getEnemyPosition(i).type)
            when (itemType) {
                EnemyType.BLOCKER -> {
                    enemies.add(
                        BlockerEnemy(
                            assetService.getEnemyPosition(i).x,
                            assetService.getEnemyPosition(i).y,
                            blockerEnemyAsset.width,
                            blockerEnemyAsset.height,
                            GameElementState.ACTIVE,
                            FrameMetadata(1, Cell(1, 1, width, height)),
                            blockerEnemyAsset.customImageWrapper,
                            EnemyType.BLOCKER,
                            assetService.getEnemyPosition(i).x,
                            assetService.getEnemyPosition(i).y,
                            EnemyPosition(
                                assetService.getEnemyPosition(i).x,
                                assetService.getEnemyPosition(i).y,
                                Direction.LEFT
                            ),
                            GameElementCollisionState.FREE,
                            EnemyInteractionState.ISOLATED
                        )
                    )
                }

                EnemyType.SHOOTER -> {
                    enemies.add(
                        ShooterEnemy(
                            assetService.getEnemyPosition(i).x,
                            assetService.getEnemyPosition(i).y,
                            shooterEnemyAsset.width,
                            shooterEnemyAsset.height,
                            GameElementState.ACTIVE,
                            FrameMetadata(1, Cell(1, 1, width, height)),
                            shooterEnemyAsset.customImageWrapper,
                            EnemyType.SHOOTER,
                            assetService.getEnemyPosition(i).x,
                            assetService.getEnemyPosition(i).y,
                            EnemyPosition(
                                assetService.getEnemyPosition(i).x,
                                assetService.getEnemyPosition(i).y,
                                Direction.LEFT
                            ),
                            GameElementCollisionState.FREE,
                            EnemyInteractionState.ISOLATED
                        )
                    )
                }

                else -> {
                    enemies.add(
                        RunnerEnemy(
                            assetService.getEnemyPosition(i).x,
                            assetService.getEnemyPosition(i).y,
                            shooterEnemyAsset.width,
                            shooterEnemyAsset.height,
                            GameElementState.INACTIVE,
                            FrameMetadata(1, Cell(1, 1, width, height)),
                            shooterEnemyAsset.customImageWrapper,
                            EnemyType.RUNNER,
                            assetService.getEnemyPosition(i).x,
                            assetService.getEnemyPosition(i).y,
                            EnemyPosition(
                                assetService.getEnemyPosition(i).x,
                                assetService.getEnemyPosition(i).y,
                                Direction.LEFT
                            ),
                            GameElementCollisionState.FREE,
                            EnemyInteractionState.ISOLATED
                        )
                    )
                }
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
