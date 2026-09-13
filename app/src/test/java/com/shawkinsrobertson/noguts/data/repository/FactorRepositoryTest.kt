package com.shawkinsrobertson.noguts.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shawkinsrobertson.noguts.data.datastore.UserPreferencesRepository
import com.shawkinsrobertson.noguts.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class FactorRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var factorRepository: FactorRepository
    private lateinit var dailyLogRepository: DailyLogRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        factorRepository = FactorRepository(database.factorDao())
        dailyLogRepository = DailyLogRepository(
            dailyLogDao = database.dailyLogDao(),
            dailyLogFactorDao = database.dailyLogFactorDao(),
            scoreSnapshotDao = database.scoreSnapshotDao(),
            factorRepository = factorRepository,
            userPreferencesRepository = UserPreferencesRepository(context)
        )
        runBlocking { factorRepository.seedDefaultsIfEmpty() }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `a factor with no logged history can be deleted outright`() = runBlocking {
        val factor = factorRepository.activeFactors.first().first { it.name == "Spicy food" }
        assertTrue(factorRepository.deleteFactor(factor))
    }

    @Test
    fun `a factor with logged history is deactivated instead of deleted`() = runBlocking {
        val factor = factorRepository.activeFactors.first().first { it.name == "Spicy food" }
        dailyLogRepository.saveDailyLog(LocalDate.of(2026, 9, 14), listOf(FactorLogInput(factor.id)), null)

        val actuallyDeleted = factorRepository.deleteFactor(factor)

        assertFalse(actuallyDeleted)
        val stillThere = factorRepository.getById(factor.id)
        assertFalse(stillThere?.active ?: true)
    }
}
