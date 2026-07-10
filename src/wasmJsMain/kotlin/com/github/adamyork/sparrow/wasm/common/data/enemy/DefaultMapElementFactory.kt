package com.github.adamyork.sparrow.wasm.common.data.enemy

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.data.*
import com.github.adamyork.sparrow.wasm.common.data.item.CollectibleItem
import com.github.adamyork.sparrow.wasm.common.data.item.ItemType
import com.github.adamyork.sparrow.wasm.service.data.ImageAsset
import com.github.adamyork.sparrow.wasm.service.data.ItemPositionAndType
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultMapElementFactory : MapElementFactory {

    override fun createCollectibleItem(
        imageAsset: ImageAsset,
        position: ItemPositionAndType,
        itemType: ItemType,
        width: Int,
        height: Int,
        id: Int,
        animationFps: Double
    ): CollectibleItem {
        return CollectibleItem(
            imageAsset.width,
            imageAsset.height,
            position.x,
            position.y,
            itemType,
            GameElementState.ACTIVE,
            imageAsset.imageAndBytes,
            FrameMetadata(1, Cell(1, 1, width, height)),
            id,
            animationFps
        )
    }

    override fun createEnemy(
        imageAsset: ImageAsset,
        position: ItemPositionAndType,
        enemyType: EnemyType,
        width: Int,
        height: Int,
        animationFps: Double
    ): Enemy {
        val state = if (enemyType == EnemyType.RUNNER) {
            GameElementState.INACTIVE
        } else {
            GameElementState.ACTIVE
        }
        return createEnemyWithState(
            imageAsset,
            position,
            enemyType,
            width,
            height,
            animationFps,
            state
        )
    }

    private fun createEnemyWithState(
        imageAsset: ImageAsset,
        position: ItemPositionAndType,
        enemyType: EnemyType,
        width: Int,
        height: Int,
        animationFps: Double,
        state: GameElementState
    ): Enemy {
        val frameMetadata = FrameMetadata(1, Cell(1, 1, width, height))
        val enemyPosition = EnemyPosition(position.x, position.y, Direction.LEFT)
        return when (enemyType) {
            EnemyType.BLOCKER -> BlockerEnemy(
                position.x, position.y, imageAsset.width, imageAsset.height, state,
                frameMetadata, imageAsset.imageAndBytes, enemyType, position.x, position.y,
                enemyPosition, GameElementCollisionState.FREE, EnemyInteractionState.ISOLATED, animationFps
            )

            EnemyType.SHOOTER -> ShooterEnemy(
                position.x, position.y, imageAsset.width, imageAsset.height, state,
                frameMetadata, imageAsset.imageAndBytes, enemyType, position.x, position.y,
                enemyPosition, GameElementCollisionState.FREE, EnemyInteractionState.ISOLATED, animationFps
            )

            EnemyType.RUNNER -> RunnerEnemy(
                position.x, position.y, imageAsset.width, imageAsset.height, state,
                frameMetadata, imageAsset.imageAndBytes, enemyType, position.x, position.y,
                enemyPosition, GameElementCollisionState.FREE, EnemyInteractionState.ISOLATED, animationFps
            )
        }
    }
}