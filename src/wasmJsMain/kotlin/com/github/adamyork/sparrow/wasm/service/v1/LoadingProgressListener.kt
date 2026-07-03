package com.github.adamyork.sparrow.wasm.service.v1

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface LoadingProgressListener {

    fun onTaskCompleted(taskId: String)

}