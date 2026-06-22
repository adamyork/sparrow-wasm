package com.github.adamyork.sparrow.wasm.dao

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.dao.data.Todo
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.reactivecircus.cache4k.Cache
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class DefaultDataFetcherDao(private val dataCache: Cache<Long, Todo>) : DataFetcherDao {

    private val logger = KotlinLogging.logger {}

    override suspend fun loadData(id: Int, bustCache: Boolean): Todo {
        return try {
            logger.info { "about to fetch todos" }
            if (bustCache) {
                fetchTodo(id)
            } else {
                dataCache.get(1) {
                    logger.info { "Cached id not preset fetching" }
                    fetchTodo(id)
                }
            }
        } catch (t: Throwable) {
            logger.error { "Request failed: ${t.message}" }
            throw t
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private suspend fun fetchTodo(id: Int): Todo {
        val response = window.fetch("https://jsonplaceholder.typicode.com/todos/${id}").await()

        if (!response.ok) {
            logger.error { "response not ok" }
            error("HTTP ${response.status}: ${response.statusText}")
        }

        val body: JsString = response.text().await()
        return Json.decodeFromString(body.toString())
    }
}
