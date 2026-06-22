package com.github.adamyork.sparrow.wasm.common

/*
 * Copyright (c) 2026. Adam York
 */
class AnimationFrameException(name: String, index: Int) :
    RuntimeException("referenced animation frame $index is missing from $name")
