package com.github.adamyork.sparrow.wasm.data.enemy

import com.github.adamyork.sparrow.wasm.CustomImageWrapper
import com.github.adamyork.sparrow.wasm.data.*
import com.github.adamyork.sparrow.wasm.data.player.Player

data class BlockerEnemy(
    override val x: Int,
    override val y: Int,
    override val width: Int,
    override val height: Int,
    override val state: GameElementState,
    override val frameMetadata: FrameMetadata,
    override val customImageWrapper: CustomImageWrapper,
    override val type: EnemyType,
    override val originX: Int,
    override val originY: Int,
    override val enemyPosition: EnemyPosition,
    override val colliding: GameElementCollisionState,
    override val interacting: EnemyInteractionState
) : Enemy {

    companion object {
        //val LOGGER: Logger = LoggerFactory.getLogger(BlockerEnemy::class.java)
        const val ANIMATION_COLLISION_FRAMES = 8
        const val MAX_X_MOVEMENT = 50
        const val MOVEMENT_X_DISTANCE = 10
    }

    var animatingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var collisionFrames: HashMap<Int, FrameMetadata> = HashMap()

    init {
        generateAnimationFrameIndex()
    }

    override fun nestedDirection(): Direction {
        return this.enemyPosition.direction
    }

    override fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState> {
        return this.getNextCollisionMetadataWithState(animatingFrames, collisionFrames)
    }

    override fun getNextPosition(): EnemyPosition {
        if (enemyPosition.direction == Direction.LEFT) {
            return if (enemyPosition.x >= originX - MAX_X_MOVEMENT) {
                EnemyPosition(
                    enemyPosition.x - MOVEMENT_X_DISTANCE,
                    enemyPosition.y,
                    Direction.LEFT
                )
            } else {
                EnemyPosition(enemyPosition.x, enemyPosition.y, Direction.RIGHT)
            }
        } else {
            return if (enemyPosition.x <= originX + MAX_X_MOVEMENT) {
                EnemyPosition(
                    enemyPosition.x + MOVEMENT_X_DISTANCE,
                    enemyPosition.y,
                    Direction.RIGHT
                )
            } else {
                EnemyPosition(enemyPosition.x, enemyPosition.y, Direction.LEFT)
            }
        }
    }

    override fun getNextEnemyState(player: Player): GameElementState {
        return this.state
    }

    @Suppress("DuplicatedCode")
    private fun generateAnimationFrameIndex() {
        animatingFrames[1] = FrameMetadata(1, Cell(1, 1, width, height))

        collisionFrames[1] = FrameMetadata(1, Cell(1, 2, width, height))
        collisionFrames[2] = FrameMetadata(2, Cell(1, 2, width, height))
        collisionFrames[3] = FrameMetadata(3, Cell(1, 1, width, height))
        collisionFrames[4] = FrameMetadata(4, Cell(1, 1, width, height))
        collisionFrames[5] = FrameMetadata(5, Cell(1, 2, width, height))
        collisionFrames[6] = FrameMetadata(6, Cell(1, 2, width, height))
        collisionFrames[7] = FrameMetadata(7, Cell(1, 1, width, height))
        collisionFrames[8] = FrameMetadata(8, Cell(1, 1, width, height))
    }

}
