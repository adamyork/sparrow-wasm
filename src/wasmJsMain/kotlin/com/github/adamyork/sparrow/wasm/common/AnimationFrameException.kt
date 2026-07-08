package com.github.adamyork.sparrow.wasm.common

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class AnimationFrameException(name: String, index: Int) :
    RuntimeException("referenced animation frame $index is missing from $name")
