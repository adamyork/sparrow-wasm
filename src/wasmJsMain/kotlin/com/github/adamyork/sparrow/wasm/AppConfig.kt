package com.github.adamyork.sparrow.wasm

import com.github.adamyork.sparrow.wasm.common.CommonConfig
import com.github.adamyork.sparrow.wasm.engine.EngineConfig
import com.github.adamyork.sparrow.wasm.gui.GuiConfig
import com.github.adamyork.sparrow.wasm.service.ServiceConfig
import me.tatarka.inject.annotations.Component

@AppScope
@Component
abstract class AppConfig : GuiConfig, ServiceConfig, EngineConfig, CommonConfig
