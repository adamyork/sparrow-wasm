package com.github.adamyork.sparrow.wasm

import com.github.adamyork.sparrow.wasm.dao.CacheConfig
import com.github.adamyork.sparrow.wasm.dao.DaoConfig
import com.github.adamyork.sparrow.wasm.gui.GuiConfig
import com.github.adamyork.sparrow.wasm.service.ServiceConfig
import me.tatarka.inject.annotations.Component

@AppScope
@Component
abstract class AppConfig : GuiConfig, DaoConfig, ServiceConfig, CacheConfig
