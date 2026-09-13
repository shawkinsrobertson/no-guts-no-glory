package com.shawkinsrobertson.noguts.data.db

import androidx.room.TypeConverter
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogStatus
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.scoring.InputType
import com.shawkinsrobertson.noguts.scoring.IntensityLevel
import java.time.Instant
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromEpochMilli(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromCategoryName(value: String?): FactorCategory? = value?.let { FactorCategory.valueOf(it) }

    @TypeConverter
    fun categoryToName(category: FactorCategory?): String? = category?.name

    @TypeConverter
    fun fromInputTypeName(value: String?): InputType? = value?.let { InputType.valueOf(it) }

    @TypeConverter
    fun inputTypeToName(inputType: InputType?): String? = inputType?.name

    @TypeConverter
    fun fromIntensityLevelName(value: String?): IntensityLevel? = value?.let { IntensityLevel.valueOf(it) }

    @TypeConverter
    fun intensityLevelToName(level: IntensityLevel?): String? = level?.name

    @TypeConverter
    fun fromDailyLogStatusName(value: String?): DailyLogStatus? = value?.let { DailyLogStatus.valueOf(it) }

    @TypeConverter
    fun dailyLogStatusToName(status: DailyLogStatus?): String? = status?.name

    /** Stored as a comma-separated list of factor ids; empty string means an empty list. */
    @TypeConverter
    fun fromFactorIdCsv(value: String?): List<Long> =
        if (value.isNullOrBlank()) emptyList() else value.split(",").map { it.toLong() }

    @TypeConverter
    fun factorIdsToCsv(ids: List<Long>?): String = ids?.joinToString(",") ?: ""
}
