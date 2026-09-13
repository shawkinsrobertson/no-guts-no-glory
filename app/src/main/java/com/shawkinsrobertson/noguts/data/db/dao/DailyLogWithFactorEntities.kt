package com.shawkinsrobertson.noguts.data.db.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogEntity
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogFactorEntity

data class DailyLogWithFactorEntities(
    @Embedded val log: DailyLogEntity,
    @Relation(parentColumn = "id", entityColumn = "dailyLogId")
    val factors: List<DailyLogFactorEntity>
)
