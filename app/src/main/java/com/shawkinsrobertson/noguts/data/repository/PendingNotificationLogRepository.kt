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

    /** Creates the pending row for [date] with its personalized factor set if one doesn't
     * already exist; otherwise leaves whatever's already in progress untouched. */
    suspend fun ensureInitialized(date: LocalDate, shownFactorIds: List<Long>): PendingNotificationLogEntity {
        val existing = dao.getByDate(date)
        if (existing != null) return existing
        val created = PendingNotificationLogEntity(
            date = date,
            selectedFactorIds = emptyList(),
            shownFactorIds = shownFactorIds,
            notes = null,
            createdAt = Instant.now()
        )
        dao.upsert(created)
        return created
    }

    suspend fun toggleFactor(date: LocalDate, factorId: Long): PendingNotificationLogEntity {
        val existing = dao.getByDate(date)
        val currentIds = existing?.selectedFactorIds ?: emptyList()
        val updatedIds = if (factorId in currentIds) currentIds - factorId else currentIds + factorId
        val updated = PendingNotificationLogEntity(
            id = existing?.id ?: 0,
            date = date,
            selectedFactorIds = updatedIds,
            shownFactorIds = existing?.shownFactorIds ?: emptyList(),
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
            shownFactorIds = existing?.shownFactorIds ?: emptyList(),
            notes = note,
            createdAt = existing?.createdAt ?: Instant.now()
        )
        dao.upsert(updated)
        return updated
    }

    suspend fun clear(date: LocalDate) = dao.deleteByDate(date)
}
