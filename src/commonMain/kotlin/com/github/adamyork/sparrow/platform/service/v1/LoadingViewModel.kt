package com.github.adamyork.sparrow.platform.service.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.adamyork.sparrow.platform.service.LoadingProgressListener
import com.github.adamyork.sparrow.platform.service.data.LoadingTask

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class LoadingViewModel : ViewModel(), LoadingProgressListener {

    companion object {
        fun mapKeyToTaskId(key: String): String {
            val fileName = if (key.startsWith("http")) {
                key.substringAfterLast("/").substringBeforeLast(".")
            } else {
                key.lowercase()
            }
            return when (fileName) {
                "player" -> "player_sprite"
                "collectible item" -> "item_sprite_1"
                "finish item" -> "item_sprite_2"
                "blocker enemy" -> "enemy_sprite_1"
                "shooter enemy" -> "enemy_sprite_2"
                "map1-bg-full-comp" -> "map_1_far"
                "map1-mg-full-comp" -> "map_1_mid"
                "map1-fg-full-comp" -> "map_1_near"
                "map1-collision" -> "map_1_col"
                "jump" -> "audio_1"
                "player_collision" -> "audio_2"
                "enemy_shoot" -> "audio_3"
                "item_collect" -> "audio_4"
                "level-1-music" -> "audio_5"
                "splash" -> "splash"
                "app_yaml" -> "app_yaml"
                "ending" -> "ending"
                "font" -> "font"
                else -> ""
            }
        }
    }

    var loadingTasks by mutableStateOf(
        listOf(
            LoadingTask("app_yaml", "Application YAML"),
            LoadingTask("player_sprite", "Player Sprite"),
            LoadingTask("enemy_sprite_1", "Enemy Sprite 1"),
            LoadingTask("enemy_sprite_2", "Enemy Sprite 2"),
            LoadingTask("item_sprite_1", "Item Sprite 1"),
            LoadingTask("item_sprite_2", "Item Sprite 2"),
            LoadingTask("map_1_far", "Map 1 Far Layer"),
            LoadingTask("map_1_mid", "Map 1 Mid Layer"),
            LoadingTask("map_1_near", "Map 1 Near Layer"),
            LoadingTask("map_1_col", "Map 1 Collision"),
            LoadingTask("audio_1", "Player Sound 1"),
            LoadingTask("audio_2", "Player Sound 2"),
            LoadingTask("audio_3", "Enemy Sound 1"),
            LoadingTask("audio_4", "Item Sound 1"),
            LoadingTask("audio_5", "Background Music"),
            LoadingTask("splash", "Splash Image"),
            LoadingTask("ending", "Ending Image"),
            LoadingTask("font", "Font")
        )
    )

    override fun onTaskCompleted(taskId: String) {
        loadingTasks = loadingTasks.map {
            if (it.id == taskId) it.copy(isCompleted = true) else it
        }
    }

}