package com.github.adamyork.sparrow.android

import com.github.adamyork.sparrow.android.common.CommonConfig
import com.github.adamyork.sparrow.android.engine.EngineConfig
import com.github.adamyork.sparrow.android.service.ServiceConfig
import com.github.adamyork.sparrow.platform.AppScope
import me.tatarka.inject.annotations.Component

@AppScope
@Component
abstract class AppConfig : CommonConfig, ServiceConfig, EngineConfig
