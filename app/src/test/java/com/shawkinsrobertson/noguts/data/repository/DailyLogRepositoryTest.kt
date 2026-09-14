package com.shawkinsrobertson.noguts.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shawkinsrobertson.noguts.data.datastore.UserPreferencesRepository
import com.shawkinsrobertson.noguts.data.db.AppDatabase
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogStatus
import com.shawkinsrobertson.noguts.scoring.IntensityLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Exercises the Room <-> scoring engine seam described in the plan's testing section:
 * zero-day logging, edit-in-place, and the missing-vs-logged-zero distinction for the
 * rolling window. Runs against an in-memory Room database via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class DailyLogRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var factorRepository: FactorRepository
    private lateinit var dailyLogRepository: DailyLogRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        factorRepository = FactorRepository(database.factorDao())
        userPreferencesRepository = UserPreferencesRepository(context)
        dailyLogRepository = DailyLogRepository(
            dailyLogDao = database.dailyLogDao(),
            dailyLogFactorDao = database.dailyLogFactorDao(),
            scoreSnapshotDao = database.scoreSnapshotDao(),
            factorRepository = factorRepository,
            userPreferencesRepository = userPreferencesRepository
        )
        runBlocking { factorRepository.seedDefaultsIfEmpty() }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `nothing notable saves a logged zero day`() = runBlocking {
        val today = LocalDate.of(2026, 9, 14)
        dailyLogRepository.saveZeroDay(today)

        val logged = dailyLogRepository.observeLogForDate(today).first()
        assertEquals(DailyLogStatus.LOGGED, logged?.status)
        assertTrue(logged?.factors.isNullOrEmpty())
    }

    @Test
    fun `an unlogged day returns null, distinct from a logged zero day`() = runBlocking {
        val unloggedDay = LocalDate.of(2026, 9, 14)
        assertNull(dailyLogRepository.observeLogForDate(unloggedDay).first())
    }

    @Test
    fun `saving a day twice edits in place rather than duplicating`() = runBlocking {
        val today = LocalDate.of(2026, 9, 14)
        val poorSleep = factorRepository.activeFactors.first().first { it.name == "Poor sleep" }

        dailyLogRepository.saveDailyLog(
            today,
            listOf(FactorLogInput(poorSleep.id, IntensityLevel.MODERATE)),
            notes = "first pass"
        )
        dailyLogRepository.saveDailyLog(
            today,
            listOf(FactorLogInput(poorSleep.id, IntensityLevel.SEVERE)),
            notes = "edited"
        )

        val logged = dailyLogRepository.observeLogForDate(today).first()
        assertEquals("edited", logged?.notes)
        assertEquals(1, logged?.factors?.size)
        assertEquals(IntensityLevel.SEVERE, logged?.factors?.first()?.level)
    }

    @Test
    fun `rolling window is building until three consecutive days are logged`() = runBlocking {
        val day1 = LocalDate.of(2026, 9, 12)
        val day2 = LocalDate.of(2026, 9, 13)

        dailyLogRepository.saveZeroDay(day1)
        dailyLogRepository.saveZeroDay(day2)
        // day3 intentionally left unlogged.

        val snapshot = dailyLogRepository.observeRecentSnapshots(10).first().first { it.date == day2 }
        assertNull(snapshot.rolling72Percent)
        assertEquals(2, snapshot.daysInRollingWindow)
    }

    @Test
    fun `rolling window establishes once three consecutive days are logged`() = runBlocking {
        val day1 = LocalDate.of(2026, 9, 12)
        val day2 = LocalDate.of(2026, 9, 13)
        val day3 = LocalDate.of(2026, 9, 14)

        dailyLogRepository.saveZeroDay(day1)
        dailyLogRepository.saveZeroDay(day2)
        dailyLogRepository.saveZeroDay(day3)

        val snapshot = dailyLogRepository.observeRecentSnapshots(10).first().first { it.date == day3 }
        assertEquals(3, snapshot.daysInRollingWindow)
        assertEquals(0.0, snapshot.rolling72Percent!!, 0.0001)
    }
}
