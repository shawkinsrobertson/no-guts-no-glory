package com.shawkinsrobertson.noguts.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shawkinsrobertson.noguts.data.datastore.UserPreferencesRepository
import com.shawkinsrobertson.noguts.data.db.AppDatabase
import com.shawkinsrobertson.noguts.scoring.IntensityLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** Covers the Stats screen's supporting queries: logged-day counts and factor selection stats. */
@RunWith(RobolectricTestRunner::class)
class DailyLogRepositoryStatsTest {

    private lateinit var database: AppDatabase
    private lateinit var factorRepository: FactorRepository
    private lateinit var dailyLogRepository: DailyLogRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        factorRepository = FactorRepository(database.factorDao())
        val userPreferencesRepository = UserPreferencesRepository(context)
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
    fun tearDown() = database.close()

    @Test
    fun `selection stats count repeated selections of the same factor across days`() = runBlocking {
        val socialLoad = factorRepository.activeFactors.first().first { it.name == "Social load" }
        val day1 = LocalDate.of(2026, 9, 10)
        val day2 = LocalDate.of(2026, 9, 11)

        dailyLogRepository.saveDailyLog(day1, listOf(FactorLogInput(socialLoad.id, IntensityLevel.MODERATE)), null)
        dailyLogRepository.saveDailyLog(day2, listOf(FactorLogInput(socialLoad.id, IntensityLevel.SEVERE)), null)

        val stats = dailyLogRepository.getSelectionStatsSince(day1)
        assertEquals(2, stats.first { it.factorId == socialLoad.id }.selectionCount)
        assertEquals(day2, stats.first { it.factorId == socialLoad.id }.lastSelectedDate)
    }

    @Test
    fun `countLoggedBetween only counts LOGGED days, not gaps`() = runBlocking {
        val day1 = LocalDate.of(2026, 9, 10)
        val day2 = LocalDate.of(2026, 9, 11)
        val day3 = LocalDate.of(2026, 9, 12) // left unlogged

        dailyLogRepository.saveZeroDay(day1)
        dailyLogRepository.saveZeroDay(day2)

        val count = dailyLogRepository.countLoggedBetween(day1, day3)
        assertEquals(2, count)
    }
}
