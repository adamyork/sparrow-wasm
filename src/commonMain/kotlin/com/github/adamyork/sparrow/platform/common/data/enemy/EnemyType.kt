package com.github.adamyork.sparrow.platform.common.data.enemy

import com.github.adamyork.sparrow.platform.common.GameDataIntegrityException

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
enum class EnemyType {
    BLOCKER,
    SHOOTER,
    RUNNER;

    companion object {
        fun from(literalValue: String): EnemyType {
            return when (literalValue) {
                "blocker" -> {
                    BLOCKER
                }

                "shooter" -> {
                    SHOOTER
                }

                "runner" -> {
                    RUNNER
                }

                else -> {
                    throw GameDataIntegrityException("Unknown map item type $literalValue")
                }
            }
        }
    }
}
