package com.github.adamyork.sparrow.platform.engine

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.data.Direction
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.engine.data.CollisionBoundaries
import com.github.adamyork.sparrow.platform.service.ScoreService
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
abstract class CommonColorCollision(
    physics: Physics,
    scoreService: ScoreService
) : BaseCollision(physics, scoreService) {


    protected lateinit var collisionMask: BooleanArray
    override var bitmapWidth: Int = 0
    protected var bitmapHeight: Int = 0
    private var lastPlayerX: Int = -1
    private var lastPlayerY: Int = -1
    private var cachedBoundaries: CollisionBoundaries? = null

    override fun cacheCollisionPixels() {
        throw EngineException("Must Implement")
    }

    override fun getCollisionBoundaries(player: Player): CollisionBoundaries {
        if (player.x == lastPlayerX && player.y == lastPlayerY && cachedBoundaries != null) {
            return cachedBoundaries!!
        }
        lastPlayerX = player.x
        lastPlayerY = player.y
        cachedBoundaries = CollisionBoundaries(
            findEdgeIterative(player.x, player, Direction.LEFT),
            findEdgeIterative(player.x, player, Direction.RIGHT),
            findCeilingIterative(player.y, player),
            findFloorIterative(player.y, player)
        )
        return cachedBoundaries!!
    }


    override fun updateCollisionXBoundaries(
        player: Player,
        collisionBoundaries: CollisionBoundaries
    ) {
        collisionBoundaries.left = findEdgeIterative(player.x, player, Direction.LEFT)
        collisionBoundaries.right = findEdgeIterative(player.x, player, Direction.RIGHT)
    }


    private fun findFloorIterative(startY: Int, player: Player): Int {
        for (y in startY until bitmapHeight) {
            if (testMaskCollision(player.x, y, player.width, 1)) return y - player.height
        }
        return bitmapHeight - player.height
    }

    private fun findCeilingIterative(startY: Int, player: Player): Int {
        for (y in startY downTo 0) {
            if (testMaskCollision(player.x, y, player.width, 1)) return y + 1
        }
        return 0
    }

    private fun findEdgeIterative(startX: Int, player: Player, direction: Direction): Int {
        val collisionEdgeInfo = getCollisionEdgeInfo(startX, player, direction)
        for (x in collisionEdgeInfo.range) {
            val checkX = if (direction == Direction.RIGHT) x + player.width - 1 else x
            if (testMaskCollision(checkX.coerceIn(0, bitmapWidth - 1), player.y, 1, player.height)) {
                return if (direction == Direction.RIGHT) x - 1 else x + 1
            }
        }
        val endPosition =
            if (direction == Direction.RIGHT) startX + collisionEdgeInfo.maxLookAhead else startX - collisionEdgeInfo.maxLookAhead
        return endPosition.coerceIn(0, collisionEdgeInfo.maxPossibleX)
    }

    private fun testMaskCollision(x: Int, y: Int, width: Int, height: Int): Boolean {
        if (x < 0 || y < 0 || x + width > bitmapWidth || y + height > bitmapHeight) return true
        for (yi in y until (y + height)) {
            val rowOffset = yi * bitmapWidth
            for (xi in x until (x + width)) {
                if (collisionMask[rowOffset + xi]) return true
            }
        }
        return false
    }


}
