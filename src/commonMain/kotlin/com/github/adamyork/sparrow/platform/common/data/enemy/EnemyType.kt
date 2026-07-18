package com.github.adamyork.sparrow.platform.common.data.enemy

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
                    throw IllegalArgumentException("Unknown map item type $literalValue")
                }
            }
        }
    }
}
