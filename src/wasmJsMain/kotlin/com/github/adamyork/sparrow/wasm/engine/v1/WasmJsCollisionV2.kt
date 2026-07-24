package com.github.adamyork.sparrow.wasm.engine.v1

import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.engine.Physics
import com.github.adamyork.sparrow.platform.engine.v2.PlatformTileCollision
import com.github.adamyork.sparrow.platform.service.ScoreService
import me.tatarka.inject.annotations.Inject

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class WasmJsCollisionV2(
    physics: Physics,
    scoreService: ScoreService
) : PlatformTileCollision(physics, scoreService)