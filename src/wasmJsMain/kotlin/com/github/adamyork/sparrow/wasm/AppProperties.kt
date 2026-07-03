package com.github.adamyork.sparrow.wasm

import kotlinx.serialization.Serializable

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@Serializable
data class GameConfig(
    val engine: EngineConfig,
    val viewport: ViewportConfig,
    val player: PlayerConfig,
    val map: MapConfig,
    val particle: ParticleConfig,
    val audio: AudioConfig
)

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@Serializable
data class EngineConfig(val fps: FpsTarget)

@Serializable
data class FpsTarget(val target: Int, val animation: Int = 12)

@Serializable
data class ViewportConfig(val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@Serializable
data class PlayerConfig(val asset: PathConfig, val width: Int, val height: Int, val x: Int, val y: Int)

@Serializable
data class MapConfig(
    val width: Int, val height: Int,
    val bg: String, val mg: String, val fg: String, val col: String,
    val collision: VisibleConfig,
    val item: ItemConfig,
    val enemy: EnemyConfig,
    val directive: DirectiveConfig
)

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@Serializable
data class VisibleConfig(val visible: Boolean)

@Serializable
data class PathConfig(val path: String)

@Serializable
data class ItemConfig(
    val asset: Map<String, AssetDimensions>, // Removed wrapper class to fix mapping
    private val position: Map<String, ItemPosition> // Changed to String keys
) {
    val positions: List<ItemPosition> by lazy {
        position.entries.sortedBy { it.key.toInt() }.map { it.value }
    }
}

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@Serializable
data class EnemyConfig(
    val asset: Map<String, AssetDimensions>, // Removed wrapper class to fix mapping
    private val position: Map<String, EnemyPosition> // Changed to String keys
) {
    val positions: List<EnemyPosition> by lazy {
        position.entries.sortedBy { it.key.toInt() }.map { it.value }
    }
}

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@Serializable
data class AssetDimensions(val width: Int, val height: Int, val path: String)

@Serializable
data class ItemPosition(val x: Int, val y: Int, val type: String, val ref: String)

@Serializable
data class EnemyPosition(val x: Int, val y: Int, val type: String, val ref: String)

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@Serializable
data class DirectiveConfig(val initial: TextColor, val finish: TextColor, val complete: TextColor)

@Serializable
data class TextColor(val text: String, val color: String)

@Serializable
data class ParticleConfig(val player: MovementCollision, val enemy: Projectile)

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@Serializable
data class MovementCollision(val movement: ColorWrapper, val collision: ColorWrapper)

@Serializable
data class ColorWrapper(val color: RGBA)

@Serializable
data class RGBA(val r: Int, val g: Int, val b: Int, val a: Int)

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@Serializable
data class Projectile(val projectile: ColorWrapper)

@Serializable
data class AudioConfig(val player: PlayerAudio, val item: CollectAudio, val background: String, val enemy: EnemyAudio)

@Serializable
data class PlayerAudio(val jump: String, val collision: String)

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@Serializable
data class CollectAudio(val collect: String)

@Serializable
data class EnemyAudio(val shoot: String)
