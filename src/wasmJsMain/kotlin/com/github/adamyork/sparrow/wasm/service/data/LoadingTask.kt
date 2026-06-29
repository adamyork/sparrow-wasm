package com.github.adamyork.sparrow.wasm.service.data

data class LoadingTask(
    val id: String,
    val label: String,
    val isCompleted: Boolean = false
)