package com.shawkinsrobertson.noguts.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shawkinsrobertson.noguts.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Covers the notification's persisted pending-state behavior (plan section 35): a tap
 * survives being read back from a fresh repository instance over the same database - the
 * closest a JVM test can get to simulating the app's process being killed between taps.
 */
@RunWith(RobolectricTestRunner::class)
class PendingNotificationLogRepositoryTest {

    private lateinit var database: AppDatabase
    private val date: LocalDate = LocalDate.of(2026, 9, 14)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun repository() = PendingNotificationLogRepository(database.pendingNotificationLogDao())

    @Test
    fun `tapping a factor selects it, tapping again deselects it`() = runBlocking {
        val repo = repository()
        repo.ensureInitialized(date, shownFactorIds = listOf(1, 2, 3))

        val afterFirstTap = repo.toggleFactor(date, factorId = 2)
        assertTrue(2L in afterFirstTap.selectedFactorIds)

        val afterSecondTap = repo.toggleFactor(date, factorId = 2)
        assertTrue(2L !in afterSecondTap.selectedFactorIds)
    }

    @Test
    fun `a note is captured onto the pending log`() = runBlocking {
        val repo = repository()
        repo.setNote(date, "Three social days in a row and no real alone time.")

        val pending = repo.get(date)
        assertEquals("Three social days in a row and no real alone time.", pending?.notes)
    }

    @Test
    fun `pending state survives a fresh repository instance over the same database`() = runBlocking {
        repository().apply {
            ensureInitialized(date, shownFactorIds = listOf(1, 2))
            toggleFactor(date, factorId = 1)
        }

        // A brand-new repository object, standing in for the process having restarted.
        val reloaded = repository().get(date)
        assertEquals(listOf(1L), reloaded?.selectedFactorIds)
        assertEquals(listOf(1L, 2L), reloaded?.shownFactorIds)
    }

    @Test
    fun `saving clears the pending state for that date`() = runBlocking {
        val repo = repository()
        repo.ensureInitialized(date, shownFactorIds = listOf(1))
        repo.toggleFactor(date, factorId = 1)

        repo.clear(date)

        assertEquals(null, repo.get(date))
    }
}
