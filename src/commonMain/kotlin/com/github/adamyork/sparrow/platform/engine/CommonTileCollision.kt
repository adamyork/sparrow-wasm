package com.github.adamyork.sparrow.platform.engine

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.data.*
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
abstract class CommonTileCollision(
    physics: Physics,
    scoreService: ScoreService
) : BaseCollision(physics, scoreService) {

    companion object {
        private const val TILE_SIZE: Int = 16
    }

    override var bitmapWidth: Int = 0
    protected var bitmapHeight: Int = 0

    private var tilesColumns: Int = 0
    private var tilesRows: Int = 0
    private lateinit var solidTileMap: BooleanArray

    private var lastPlayerX: Int = -1
    private var lastPlayerY: Int = -1
    private var cachedBoundaries: CollisionBoundaries? = null

    override fun cacheCollisionPixels() {
        throw EngineException("Must Implement")
    }

    protected fun populateTileMapFromPixelSource(
        width: Int,
        height: Int,
        isCollisionPixel: (Int, Int) -> Boolean
    ) {
        bitmapWidth = width
        bitmapHeight = height
        tilesColumns = (bitmapWidth + TILE_SIZE - 1) / TILE_SIZE
        tilesRows = (bitmapHeight + TILE_SIZE - 1) / TILE_SIZE
        solidTileMap = BooleanArray(tilesColumns * tilesRows)

        for (row in 0 until tilesRows) {
            for (col in 0 until tilesColumns) {
                val startX = col * TILE_SIZE
                val startY = row * TILE_SIZE
                val endX = (startX + TILE_SIZE).coerceAtMost(bitmapWidth)
                val endY = (startY + TILE_SIZE).coerceAtMost(bitmapHeight)

                var tileIsSolid = false
                for (y in startY until endY) {
                    for (x in startX until endX) {
                        if (isCollisionPixel(x, y)) {
                            tileIsSolid = true
                            break
                        }
                    }
                    if (tileIsSolid) break
                }
                solidTileMap[row * tilesColumns + col] = tileIsSolid
            }
        }
    }

    override fun getCollisionBoundaries(player: Player): CollisionBoundaries {
        if (player.x == lastPlayerX && player.y == lastPlayerY && cachedBoundaries != null) {
            return cachedBoundaries!!
        }
        lastPlayerX = player.x
        lastPlayerY = player.y
        cachedBoundaries = CollisionBoundaries(
            findEdgeTile(player.x, player, Direction.LEFT),
            findEdgeTile(player.x, player, Direction.RIGHT),
            findCeilingTile(player.y, player),
            findFloorTile(player.y, player)
        )
        return cachedBoundaries!!
    }

    override fun updateCollisionXBoundaries(
        player: Player,
        collisionBoundaries: CollisionBoundaries
    ) {
        collisionBoundaries.left = findEdgeTile(player.x, player, Direction.LEFT)
        collisionBoundaries.right = findEdgeTile(player.x, player, Direction.RIGHT)
    }

    private fun findFloorTile(startY: Int, player: Player): Int {
        val playerBottom = startY + player.height
        val startTileY = playerBottom / TILE_SIZE
        val endTileY = (bitmapHeight - 1) / TILE_SIZE

        for (tileY in startTileY..endTileY) {
            val tileTopY = tileY * TILE_SIZE
            if (testTileCollision(player.x, tileTopY, player.width, 1)) {
                return tileTopY - player.height
            }
        }
        return bitmapHeight - player.height
    }

    private fun findCeilingTile(startY: Int, player: Player): Int {
        val startTileY = startY / TILE_SIZE
        for (tileY in startTileY downTo 0) {
            val tileBottomY = (tileY + 1) * TILE_SIZE
            if (testTileCollision(player.x, tileY * TILE_SIZE, player.width, 1)) {
                return tileBottomY
            }
        }
        return 0
    }

    private fun findEdgeTile(startX: Int, player: Player, direction: Direction): Int {
        val collisionEdgeInfo = getCollisionEdgeInfo(startX, player, direction)
        for (x in collisionEdgeInfo.range) {
            val checkX = if (direction == Direction.RIGHT) x + player.width - 1 else x
            if (testTileCollision(checkX.coerceIn(0, bitmapWidth - 1), player.y, 1, player.height)) {
                return if (direction == Direction.RIGHT) x - 1 else x + 1
            }
        }
        val endPosition =
            if (direction == Direction.RIGHT) startX + collisionEdgeInfo.maxLookAhead else startX - collisionEdgeInfo.maxLookAhead
        return endPosition.coerceIn(0, collisionEdgeInfo.maxPossibleX)
    }

    private fun testTileCollision(x: Int, y: Int, width: Int, height: Int): Boolean {
        if (x < 0 || y < 0 || x + width > bitmapWidth || y + height > bitmapHeight) return true

        val startCol = x / TILE_SIZE
        val endCol = (x + width - 1) / TILE_SIZE
        val startRow = y / TILE_SIZE
        val endRow = (y + height - 1) / TILE_SIZE

        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                if (row in 0 until tilesRows && col in 0 until tilesColumns) {
                    if (solidTileMap[row * tilesColumns + col]) {
                        return true
                    }
                }
            }
        }
        return false
    }

}
