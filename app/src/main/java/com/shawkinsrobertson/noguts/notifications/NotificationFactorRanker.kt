package com.shawkinsrobertson.noguts.notifications

import com.shawkinsrobertson.noguts.data.db.dao.FactorSelectionStat
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.scoring.FactorCategory

private const val LOAD_SLOTS = 4
private const val MIN_RECOVERY_SLOTS = 1
private const val MAX_RECOVERY_SLOTS = 2

/**
 * Picks which factors appear on the interactive notification (plan section 24): the most
 * commonly/recently selected active factors, ranked per-category so recovery options
 * can't be crowded out entirely by load factors ("a diversity rule so the notification
 * does not become entirely load-focused").
 *
 * With no usage history yet, everything ties on selection count/recency and the ranking
 * falls back to each factor's [FactorEntity.sortOrder] - the same "most relevant" default
 * used on the Dashboard.
 */
object NotificationFactorRanker {

    fun rank(activeFactors: List<FactorEntity>, stats: List<FactorSelectionStat>): List<FactorEntity> {
        val statsByFactorId = stats.associateBy { it.factorId }

        fun rankedWithin(category: FactorCategory, limit: Int): List<FactorEntity> =
            activeFactors
                .filter { it.category == category }
                .sortedWith(
                    compareByDescending<FactorEntity> { statsByFactorId[it.id]?.selectionCount ?: 0 }
                        .thenByDescending { statsByFactorId[it.id]?.lastSelectedDate }
                        .thenBy { it.sortOrder }
                )
                .take(limit)

        val loadPicks = rankedWithin(FactorCategory.LOAD, LOAD_SLOTS)
        val recoveryPicks = rankedWithin(FactorCategory.RECOVERY, MAX_RECOVERY_SLOTS)
            .let { if (it.size < MIN_RECOVERY_SLOTS) rankedWithin(FactorCategory.RECOVERY, MIN_RECOVERY_SLOTS) else it }

        return loadPicks + recoveryPicks
    }
}
