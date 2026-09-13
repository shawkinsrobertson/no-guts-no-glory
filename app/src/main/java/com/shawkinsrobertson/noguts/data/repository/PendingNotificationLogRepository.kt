package com.shawkinsrobertson.noguts.data.repository

import com.shawkinsrobertson.noguts.data.db.dao.PendingNotificationLogDao
import com.shawkinsrobertson.noguts.data.db.entity.PendingNotificationLogEntity
import java.time.Instant
import java.time.LocalDate

/**
 * Persists the in-progress state of the interactive daily notification so a tap survives
 * the app's process being killed between taps (plan section 23).
 */
class PendingNotificationLogRepository(private val dao: PendingNotificationLogDao) {

    suspend fun get(date: LocalDate): PendingNotificationLogEntity? = dao.getByDate(date)

    suspend fun toggleFactor(date: LocalDate, factorId: Long): PendingNotificationLogEntity {
        val existing = dao.getByDate(date)
        val currentIds = existing?.selectedFactorIds ?: emptyList()
        val updatedIds = if (factorId in currentIds) currentIds - factorId else currentIds + factorId
        val updated = PendingNotificationLogEntity(
            id = existing?.id ?: 0,
            date = date,
            selectedFactorIds = updatedIds,
            notes = existing?.notes,
            createdAt = existing?.createdAt ?: Instant.now()
        )
        dao.upsert(updated)
        return updated
    }

    suspend fun setNote(date: LocalDate, note: String): PendingNotificationLogEntity {
        val existing = dao.getByDate(date)
        val updated = PendingNotificationLogEntity(
            id = existing?.id ?: 0,
            date = date,
            selectedFactorIds = existing?.selectedFactorIds ?: emptyList(),
            notes = note,
            createdAt = existing?.createdAt ?: Instant.now()
        )
        dao.upsert(updated)
        return updated
    }

    suspend fun clear(date: LocalDate) = dao.deleteByDate(date)
}
