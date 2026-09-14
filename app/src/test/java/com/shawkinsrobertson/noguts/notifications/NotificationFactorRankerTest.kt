package com.shawkinsrobertson.noguts.notifications

import com.shawkinsrobertson.noguts.data.db.dao.FactorSelectionStat
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.scoring.InputType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

private fun factor(id: Long, name: String, category: FactorCategory, sortOrder: Int = 0) = FactorEntity(
    id = id,
    name = name,
    category = category,
    inputType = InputType.BOOLEAN,
    weight = 5.0,
    active = true,
    sortOrder = sortOrder,
    isSystemDefault = true,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH
)

class NotificationFactorRankerTest {

    @Test
    fun `with no usage history falls back to sortOrder`() {
        val factors = listOf(
            factor(1, "Rare food trigger", FactorCategory.LOAD, sortOrder = 9),
            factor(2, "Poor sleep", FactorCategory.LOAD, sortOrder = 0),
            factor(3, "Full recovery day", FactorCategory.RECOVERY, sortOrder = 6)
        )

        val ranked = NotificationFactorRanker.rank(factors, emptyList())

        assertEquals("Poor sleep", ranked.first().name)
        assertTrue("recovery must always be represented", ranked.any { it.category == FactorCategory.RECOVERY })
    }

    @Test
    fun `frequently and recently selected factors outrank the default sort order`() {
        val neverSelected = factor(1, "Rarely relevant", FactorCategory.LOAD, sortOrder = 0)
        val heavilyUsed = factor(2, "Social load", FactorCategory.LOAD, sortOrder = 9)
        val stats = listOf(FactorSelectionStat(factorId = 2, selectionCount = 10, lastSelectedDate = LocalDate.now()))

        val ranked = NotificationFactorRanker.rank(listOf(neverSelected, heavilyUsed), stats)

        assertEquals("Social load", ranked.first().name)
    }

    @Test
    fun `recovery is never crowded out even with many load factors`() {
        val loadFactors = (1..8).map { factor(it.toLong(), "Load $it", FactorCategory.LOAD) }
        val recovery = factor(100, "Full recovery day", FactorCategory.RECOVERY)

        val ranked = NotificationFactorRanker.rank(loadFactors + recovery, emptyList())

        assertTrue(ranked.contains(recovery))
    }
}
