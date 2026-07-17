package com.github.adamyork.sparrow.platform.common

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class AnimationFrameException(name: String, index: Int) :
    RuntimeException("referenced animation frame $index is missing from $name")
