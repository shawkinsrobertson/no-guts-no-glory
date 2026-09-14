package com.shawkinsrobertson.noguts.data.repository

import android.content.Context
import androidx.core.content.FileProvider
import android.net.Uri
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import java.io.File

/**
 * Exports raw observations rather than just scores (plan section 30) - the goal is for
 * Julie to own the underlying data, not merely a graph of it. Two CSVs are produced: one
 * row per factor observation, and one row per day's calculated scores.
 */
class CsvExportRepository(
    private val context: Context,
    private val dailyLogRepository: DailyLogRepository
) {

    suspend fun exportToShareableUris(): List<Uri> {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val observationsFile = File(exportsDir, "observations.csv").apply { writeText(buildObservationsCsv()) }
        val scoresFile = File(exportsDir, "scores.csv").apply { writeText(buildScoresCsv()) }

        val authority = "${context.packageName}.fileprovider"
        return listOf(
            FileProvider.getUriForFile(context, authority, observationsFile),
            FileProvider.getUriForFile(context, authority, scoresFile)
        )
    }

    private suspend fun buildObservationsCsv(): String = buildString {
        appendLine("date,factor,category,level,weight,points,notes")
        dailyLogRepository.getAllLoggedDaysOnce().sortedBy { it.date }.forEach { day ->
            val notes = day.notes?.let { csvField(it) } ?: ""
            if (day.factors.isEmpty()) {
                appendLine("${day.date},(nothing notable),,,,0,$notes")
            } else {
                day.factors.forEach { detail ->
                    val level = detail.level?.name ?: "YES"
                    // Recovery's raw contribution is shown as a negative offset for
                    // readability, matching the plan's worked example; the actual capped
                    // offset applied to that day's net load is a whole-day calculation,
                    // not something attributable to a single factor.
                    val points = if (detail.factor.category == FactorCategory.RECOVERY) {
                        -detail.calculatedPoints
                    } else {
                        detail.calculatedPoints
                    }
                    appendLine(
                        "${day.date},${csvField(detail.factor.name)},${detail.factor.category}," +
                            "$level,${detail.factor.weight},$points,$notes"
                    )
                }
            }
        }
    }

    private suspend fun buildScoresCsv(): String = buildString {
        appendLine("date,dailyLoad,normalizedDailyPercent,rolling72Load,rolling72Percent,targetPercent")
        dailyLogRepository.getAllSnapshotsOnce().sortedBy { it.date }.forEach { snapshot ->
            appendLine(
                "${snapshot.date},${snapshot.dailyLoad},${snapshot.normalizedDailyPercent}," +
                    "${snapshot.rolling72Load ?: ""},${snapshot.rolling72Percent ?: ""},${snapshot.targetPercent}"
            )
        }
    }

    private fun csvField(value: String): String =
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
}
