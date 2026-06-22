package com.github.adamyork.sparrow.wasm.dao

import com.github.adamyork.sparrow.wasm.AppScope
import me.tatarka.inject.annotations.Provides

interface DaoConfig {

    val dataFetcherDao: DataFetcherDao

    @AppScope
    @Provides
    fun provideDataFetcherDao(impl: DefaultDataFetcherDao): DataFetcherDao = impl

}
