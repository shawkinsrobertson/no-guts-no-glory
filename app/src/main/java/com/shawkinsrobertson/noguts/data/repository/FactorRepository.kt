package com.shawkinsrobertson.noguts.data.repository

import com.shawkinsrobertson.noguts.data.db.dao.FactorDao
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.scoring.calculateMaxPossibleDailyLoad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant

class FactorRepository(private val factorDao: FactorDao) {

    val allFactors: Flow<List<FactorEntity>> = factorDao.observeAll()
    val activeFactors: Flow<List<FactorEntity>> = factorDao.observeActive()

    suspend fun getActiveFactors(): List<FactorEntity> = factorDao.getActive()

    /** Seeds the default factor library on first run only; a no-op afterward. */
    suspend fun seedDefaultsIfEmpty() {
        if (factorDao.count() == 0) {
            factorDao.insertAll(DefaultFactors.seedEntities())
        }
    }

    suspend fun getById(id: Long): FactorEntity? = factorDao.getById(id)

    suspend fun getByIds(ids: List<Long>): List<FactorEntity> = factorDao.getByIds(ids)

    /** The current maximum possible daily load, from currently active LOAD factors only. */
    suspend fun currentMaxPossibleDailyLoad(): Double {
        val activeLoadWeights = factorDao.getActive()
            .filter { it.category == FactorCategory.LOAD }
            .map { it.weight }
        return calculateMaxPossibleDailyLoad(activeLoadWeights)
    }

    suspend fun addCustomFactor(
        name: String,
        category: FactorCategory,
        inputType: com.shawkinsrobertson.noguts.scoring.InputType,
        weight: Double
    ): Long {
        val now = Instant.now()
        val nextSortOrder = (allFactors.first().maxOfOrNull { it.sortOrder } ?: 0) + 1
        return factorDao.insert(
            FactorEntity(
                name = name,
                category = category,
                inputType = inputType,
                weight = weight,
                active = true,
                sortOrder = nextSortOrder,
                isSystemDefault = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateFactor(factor: FactorEntity) {
        factorDao.update(factor.copy(updatedAt = Instant.now()))
    }

    suspend fun setActive(factor: FactorEntity, active: Boolean) {
        factorDao.update(factor.copy(active = active, updatedAt = Instant.now()))
    }

    /** Returns true if the factor was actually deleted, false if it had logged history and
     * was deactivated instead (a factor with history can't be hard-deleted - see the FK on
     * DailyLogFactorEntity). */
    suspend fun deleteFactor(factor: FactorEntity): Boolean = try {
        factorDao.delete(factor)
        true
    } catch (e: android.database.sqlite.SQLiteConstraintException) {
        factorDao.update(factor.copy(active = false, updatedAt = Instant.now()))
        false
    }
}
