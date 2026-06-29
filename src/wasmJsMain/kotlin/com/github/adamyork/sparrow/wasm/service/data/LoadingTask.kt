package com.github.adamyork.sparrow.wasm.service.data

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class LoadingTask(
    val id: String,
    val label: String,
    val isCompleted: Boolean = false
)