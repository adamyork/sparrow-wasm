package com.github.adamyork.sparrow.wasm.service

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.dao.DefaultDataFetcherDao
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.tatarka.inject.annotations.Inject
import kotlin.random.Random

@AppScope
@Inject
class DefaultRandomNumberService(
    private val dataFetcherDao: DefaultDataFetcherDao
) : RandomNumberService {

    private val logger = KotlinLogging.logger {}

    override suspend fun getRandomNumber(): Int {
        logger.info { "Get Random Number" }
        val nextInt = Random.nextInt(100)
        val id = dataFetcherDao.loadData(nextInt, true).id
        logger.info { "Number is $id" }
        return id
    }

    override suspend fun getRandomNumberCached(): Int {
        logger.info { "Get cached Number" }
        val nextInt = Random.nextInt(100)
        return dataFetcherDao.loadData(nextInt, false).id
    }

    override suspend fun getRandomNumbersAndSum(): Int = coroutineScope {
        val first = async { dataFetcherDao.loadData(Random.nextInt(100), true).id }
        val second = async { dataFetcherDao.loadData(Random.nextInt(100), true).id }
        awaitAll(first, second).sum()
    }
}
