package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.local.HistoryDao
import com.qihe.clipflow.data.local.HistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {

    fun getAll(): Flow<List<HistoryEntity>> = historyDao.getAllFlow()

    fun getByPlatform(platform: String): Flow<List<HistoryEntity>> = historyDao.getByPlatform(platform)

    fun search(query: String): Flow<List<HistoryEntity>> = historyDao.search(query)

    suspend fun insert(entity: HistoryEntity): Long = historyDao.insert(entity)

    suspend fun getByUrl(url: String): HistoryEntity? = historyDao.getByUrl(url)

    suspend fun updateTimestamp(url: String, timestamp: Long) = historyDao.updateTimestamp(url, timestamp)

    suspend fun delete(entity: HistoryEntity) = historyDao.delete(entity)

    suspend fun deleteByIds(ids: List<Long>) = historyDao.deleteByIds(ids)

    suspend fun deleteAll() = historyDao.deleteAll()

    suspend fun count(): Int = historyDao.count()
}
