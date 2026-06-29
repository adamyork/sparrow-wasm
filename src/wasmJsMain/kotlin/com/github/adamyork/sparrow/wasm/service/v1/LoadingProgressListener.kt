package com.github.adamyork.sparrow.wasm.service.v1

interface LoadingProgressListener {
    fun onTaskCompleted(taskId: String)
}