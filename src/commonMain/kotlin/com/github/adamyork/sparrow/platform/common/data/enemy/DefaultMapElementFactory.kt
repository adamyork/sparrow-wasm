package com.github.adamyork.sparrow.platform.common.data.enemy

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.*
import com.github.adamyork.sparrow.platform.common.data.item.CollectibleItem
import com.github.adamyork.sparrow.platform.common.data.item.ItemType
import com.github.adamyork.sparrow.platform.service.data.ImageAsset
import com.github.adamyork.sparrow.platform.service.data.ItemPositionAndType
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultMapElementFactory(
    private val platformInterop: PlatformInterop
) : MapElementFactory {

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
            ElementState.ACTIVE,
            imageAsset.imageAndBytes,
            FrameMetadata(1, Cell(1, 1, width, height)),
            id,
            platformInterop,
            animationFps
        )
    }

    override fun createEnemy(
        imageAsset: ImageAsset,
        position: ItemPositionAndType,
        enemyType: EnemyType,
        width: Int,
        height: Int,
        id: Int,
        animationFps: Double
    ): Enemy {
        val state = if (enemyType == EnemyType.RUNNER) {
            ElementState.INACTIVE
        } else {
            ElementState.ACTIVE
        }
        return createEnemyWithState(
            imageAsset,
            position,
            enemyType,
            width,
            height,
            id,
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
        id: Int,
        animationFps: Double,
        state: ElementState
    ): Enemy {
        val frameMetadata = FrameMetadata(1, Cell(1, 1, width, height))
        val enemyPosition = EnemyPosition(position.x, position.y, Direction.LEFT)
        return when (enemyType) {
            EnemyType.BLOCKER -> BlockerEnemy(
                position.x, position.y, imageAsset.width, imageAsset.height, state,
                frameMetadata, imageAsset.imageAndBytes, id, enemyType, position.x, position.y,
                enemyPosition, GameElementCollisionState.FREE, EnemyInteractionState.ISOLATED, platformInterop,
                animationFps
            )

            EnemyType.SHOOTER -> ShooterEnemy(
                position.x, position.y, imageAsset.width, imageAsset.height, state,
                frameMetadata, imageAsset.imageAndBytes, id, enemyType, position.x, position.y,
                enemyPosition, GameElementCollisionState.FREE, EnemyInteractionState.ISOLATED, platformInterop,
                animationFps
            )

            EnemyType.RUNNER -> RunnerEnemy(
                position.x, position.y, imageAsset.width, imageAsset.height, state,
                frameMetadata, imageAsset.imageAndBytes, id, enemyType, position.x, position.y,
                enemyPosition, GameElementCollisionState.FREE, EnemyInteractionState.ISOLATED, platformInterop,
                animationFps
            )
        }
    }
}
