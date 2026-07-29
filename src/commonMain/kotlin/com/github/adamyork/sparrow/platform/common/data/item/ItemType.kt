package com.github.adamyork.sparrow.platform.common.data.item

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
enum class ItemType {
    COLLECTABLE,
    FINISH;

    companion object {
        fun from(literalValue: String): ItemType {
            return when (literalValue) {
                "collectable" -> {
                    COLLECTABLE
                }

                "finish" -> {
                    FINISH
                }

                else -> {
                    // TODO custom error
                    throw IllegalArgumentException("Unknown map item type $literalValue")
                }
            }
        }
    }


}
