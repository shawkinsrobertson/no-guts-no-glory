package com.shawkinsrobertson.noguts.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FactorDao {
    @Query("SELECT * FROM factors ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<FactorEntity>>

    @Query("SELECT * FROM factors WHERE active = 1 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<FactorEntity>>

    @Query("SELECT * FROM factors WHERE active = 1 ORDER BY sortOrder ASC, name ASC")
    suspend fun getActive(): List<FactorEntity>

    @Query("SELECT * FROM factors WHERE id = :id")
    suspend fun getById(id: Long): FactorEntity?

    @Query("SELECT * FROM factors WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<FactorEntity>

    @Query("SELECT COUNT(*) FROM factors")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(factor: FactorEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(factors: List<FactorEntity>)

    @Update
    suspend fun update(factor: FactorEntity)

    @Delete
    suspend fun delete(factor: FactorEntity)
}
