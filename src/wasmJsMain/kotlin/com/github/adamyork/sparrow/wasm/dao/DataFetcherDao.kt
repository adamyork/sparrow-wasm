package com.github.adamyork.sparrow.wasm.dao

import com.github.adamyork.sparrow.wasm.dao.data.Todo

interface DataFetcherDao {

    suspend fun loadData(id: Int, bustCache: Boolean): Todo

}
