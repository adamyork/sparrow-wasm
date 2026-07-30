package com.github.adamyork.sparrow.platform.common.data.item

import com.github.adamyork.sparrow.platform.common.GameDataIntegrityException

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
                    throw GameDataIntegrityException("Unknown map item type $literalValue")
                }
            }
        }
    }


}
