package com.shawkinsrobertson.noguts.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogStatus
import com.shawkinsrobertson.noguts.data.db.entity.ScoreSnapshotEntity
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.scoring.LoadTier
import com.shawkinsrobertson.noguts.scoring.loadTierFor
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val PAGE_WIDTH_PT = 612f
private const val PAGE_HEIGHT_PT = 792f
private const val MARGIN_PT = 40f
private const val CONTENT_WIDTH_PT = PAGE_WIDTH_PT - 2 * MARGIN_PT

/** Above this many days in the range, rows aggregate by week (with a mini gauge) instead
 * of one row per day - a 90-day range as 90 individual rows isn't a "report" anymore. */
private const val WEEKLY_BUCKET_THRESHOLD_DAYS = 28
private const val TOP_FACTOR_COUNT = 5

private const val ACCENT_COLOR = 0xFF2E7D6B.toInt()
private const val TRACK_COLOR = 0xFFE3E3E3.toInt()
private const val DIVIDER_COLOR = 0xFFE6E6E6.toInt()
private const val TEXT_PRIMARY_COLOR = 0xFF1B1B1B.toInt()
private const val TEXT_SECONDARY_COLOR = 0xFF6E6E6E.toInt()
private val GAUGE_TIER_COLORS = mapOf(
    LoadTier.GREEN to 0xFF3E9C6B.toInt(),
    LoadTier.YELLOW to 0xFFE3A526.toInt(),
    LoadTier.RED to 0xFFD1543E.toInt()
)

private const val GAUGE_START_ANGLE_DEG = 135f
private const val GAUGE_SWEEP_MAX_DEG = 270f

private val HEADER_DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val ROW_DATE_FMT = DateTimeFormatter.ofPattern("MMM d")

/**
 * A printable/shareable PDF companion to the CSV export (plan section 30 covers "own your
 * data"; this is the "actually look at it" counterpart) - a trend chart, a table of the
 * selected range (per day for shorter ranges, per week with a mini load gauge for longer
 * ones), and a most-common-factors summary. Reads the same repositories as the rest of the
 * app; draws with plain android.graphics onto a PdfDocument rather than Compose, since a
 * PDF page has no Compose runtime to render into.
 */
class PdfReportRepository(
    private val context: Context,
    private val dailyLogRepository: DailyLogRepository,
    private val factorRepository: FactorRepository
) {

    suspend fun exportToShareableUri(range: ReportDateRange): Uri {
        val today = LocalDate.now()
        val start = range.startDate(today)
        val end = today

        val loggedDaysByDate = dailyLogRepository.getLoggedDaysBetween(start, end).associateBy { it.date }
        val snapshotsByDate = dailyLogRepository.getSnapshotsBetween(start, end).associateBy { it.date }
        val stats = dailyLogRepository.getSelectionStatsBetween(start, end)
        val trackableFactorsById = factorRepository.getActiveFactors()
            .filter { it.category != FactorCategory.SYMPTOM }
            .associateBy { it.id }

        val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
        val rows: List<ReportRow> = if (totalDays > WEEKLY_BUCKET_THRESHOLD_DAYS) {
            buildWeeklyRows(start, end, snapshotsByDate)
        } else {
            buildDailyRows(start, end, snapshotsByDate, loggedDaysByDate)
        }

        val trendPoints = buildList {
            var d = start
            while (!d.isAfter(end)) {
                snapshotsByDate[d]?.rolling72Load?.let { add(d to it) }
                d = d.plusDays(1)
            }
        }

        val topFactors = stats
            .filter { it.factorId in trackableFactorsById.keys }
            .sortedByDescending { it.selectionCount }
            .take(TOP_FACTOR_COUNT)
            .mapNotNull { stat -> trackableFactorsById[stat.factorId]?.let { TopFactor(it.name, stat.selectionCount) } }

        val loggedDayCount = loggedDaysByDate.values.count { it.status == DailyLogStatus.LOGGED }

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "report.pdf")
        renderPdf(file, range, start, end, rows, trendPoints, topFactors, loggedDayCount, totalDays)

        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    // ---- Data shaping ----

    private sealed class ReportRow {
        data class Day(val label: String, val points: Double?, val summary: String) : ReportRow()
        data class Week(val label: String, val averagePercent: Double?, val daysLogged: Int, val daysInBucket: Int) : ReportRow()
    }

    private data class TopFactor(val name: String, val selectionCount: Int)

    private fun buildDailyRows(
        start: LocalDate,
        end: LocalDate,
        snapshotsByDate: Map<LocalDate, ScoreSnapshotEntity>,
        loggedDaysByDate: Map<LocalDate, LoggedDay>
    ): List<ReportRow.Day> {
        val rows = mutableListOf<ReportRow.Day>()
        var d = start
        while (!d.isAfter(end)) {
            val loggedDay = loggedDaysByDate[d]
            rows += ReportRow.Day(
                label = ROW_DATE_FMT.format(d),
                points = snapshotsByDate[d]?.dailyLoad,
                summary = daySummary(loggedDay)
            )
            d = d.plusDays(1)
        }
        return rows
    }

    private fun daySummary(loggedDay: LoggedDay?): String = when {
        loggedDay == null -> "Not logged"
        loggedDay.factors.isEmpty() -> "Nothing notable"
        else -> {
            val names = loggedDay.factors
                .filter { it.factor.category != FactorCategory.SYMPTOM }
                .map { it.factor.name }
            if (names.isEmpty()) "(symptoms only)" else names.joinToString(", ")
        }
    }

    /** Simple fixed 7-day buckets counted forward from [start] - not aligned to calendar
     * weeks, so a custom range's first/last bucket is never an awkward partial week
     * relative to some other reference point. The last bucket may be shorter than 7 days
     * if the range doesn't divide evenly. */
    private fun buildWeeklyRows(
        start: LocalDate,
        end: LocalDate,
        snapshotsByDate: Map<LocalDate, ScoreSnapshotEntity>
    ): List<ReportRow.Week> {
        val rows = mutableListOf<ReportRow.Week>()
        var weekStart = start
        while (!weekStart.isAfter(end)) {
            val weekEnd = minOf(weekStart.plusDays(6), end)
            val percentsInWeek = mutableListOf<Double>()
            var daysInBucket = 0
            var d = weekStart
            while (!d.isAfter(weekEnd)) {
                daysInBucket++
                snapshotsByDate[d]?.let { percentsInWeek.add(it.normalizedDailyPercent) }
                d = d.plusDays(1)
            }
            val label = if (weekStart == weekEnd) {
                ROW_DATE_FMT.format(weekStart)
            } else {
                "${ROW_DATE_FMT.format(weekStart)} - ${ROW_DATE_FMT.format(weekEnd)}"
            }
            rows += ReportRow.Week(
                label = label,
                averagePercent = if (percentsInWeek.isEmpty()) null else percentsInWeek.average(),
                daysLogged = percentsInWeek.size,
                daysInBucket = daysInBucket
            )
            weekStart = weekStart.plusDays(7)
        }
        return rows
    }

    // ---- Drawing ----

    private fun renderPdf(
        file: File,
        range: ReportDateRange,
        start: LocalDate,
        end: LocalDate,
        rows: List<ReportRow>,
        trendPoints: List<Pair<LocalDate, Double>>,
        topFactors: List<TopFactor>,
        loggedDayCount: Int,
        totalDays: Int
    ) {
        val document = PdfDocument()
        val cursor = PageCursor(document)
        val isWeekly = rows.isNotEmpty() && rows.first() is ReportRow.Week

        drawTitleBlock(cursor, range, start, end)

        if (trendPoints.size >= 2) {
            drawTrendChartBlock(cursor, trendPoints)
        }

        drawTableSection(cursor, rows, isWeekly)
        drawTopFactorsSection(cursor, topFactors, loggedDayCount, totalDays)

        cursor.finishCurrentPage()

        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
    }

    private fun drawTitleBlock(cursor: PageCursor, range: ReportDateRange, start: LocalDate, end: LocalDate) {
        cursor.ensureSpace(72f)
        val canvas = cursor.canvas()
        canvas.drawText("No Guts No Glory", MARGIN_PT, cursor.y + 22f, titlePaint())
        canvas.drawText("Load report - ${range.label}", MARGIN_PT, cursor.y + 42f, subtitlePaint())
        val rangeText = "${HEADER_DATE_FMT.format(start)} - ${HEADER_DATE_FMT.format(end)}  |  generated ${HEADER_DATE_FMT.format(LocalDate.now())}"
        canvas.drawText(rangeText, MARGIN_PT, cursor.y + 58f, captionPaint())
        canvas.drawText(
            "A personal tracking tool. Not a medical measurement or diagnosis.",
            MARGIN_PT, cursor.y + 72f, captionPaint()
        )
        cursor.advance(92f)
    }

    private fun drawTrendChartBlock(cursor: PageCursor, trendPoints: List<Pair<LocalDate, Double>>) {
        val chartHeight = 150f
        cursor.ensureSpace(chartHeight + 34f)
        val canvas = cursor.canvas()
        canvas.drawText("72-hour rolling load", MARGIN_PT, cursor.y + 12f, sectionHeaderPaint())
        val chartRect = RectF(MARGIN_PT, cursor.y + 22f, MARGIN_PT + CONTENT_WIDTH_PT, cursor.y + 22f + chartHeight)
        drawTrendChart(canvas, trendPoints, chartRect)
        cursor.advance(chartHeight + 34f)
    }

    private fun drawTrendChart(canvas: Canvas, points: List<Pair<LocalDate, Double>>, rect: RectF) {
        canvas.drawRect(rect, gridPaint())
        val highest = points.maxOf { it.second }
        val maxY = (highest * 1.1).coerceAtLeast(1.0)
        val stepX = rect.width() / (points.size - 1)
        fun yFor(value: Double): Float = rect.bottom - (value / maxY * rect.height()).toFloat()

        val path = Path()
        points.forEachIndexed { index, (_, value) ->
            val x = rect.left + stepX * index
            val y = yFor(value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, linePaint())

        // A handful of value labels along the line rather than every point, to avoid
        // overlapping text on a long range.
        val labelEvery = (points.size / 6).coerceAtLeast(1)
        points.forEachIndexed { index, (date, value) ->
            if (index % labelEvery != 0 && index != points.lastIndex) return@forEachIndexed
            val x = rect.left + stepX * index
            canvas.drawText(ROW_DATE_FMT.format(date), x, rect.bottom + 14f, captionPaint())
        }
    }

    private fun drawTableSection(cursor: PageCursor, rows: List<ReportRow>, isWeekly: Boolean) {
        cursor.ensureSpace(24f)
        drawTableHeader(cursor, isWeekly)

        if (rows.isEmpty()) {
            val canvas = cursor.canvas()
            canvas.drawText("Nothing logged in this range yet.", MARGIN_PT, cursor.y + 14f, bodyPaint())
            cursor.advance(24f)
            return
        }

        rows.forEach { row ->
            val rowHeight = if (row is ReportRow.Week) 46f else 20f
            if (cursor.wouldOverflow(rowHeight)) {
                cursor.startNewPage()
                drawTableHeader(cursor, isWeekly)
            }
            drawTableRow(cursor, row)
            cursor.advance(rowHeight)
        }
    }

    private fun drawTableHeader(cursor: PageCursor, isWeekly: Boolean) {
        val canvas = cursor.canvas()
        val label = if (isWeekly) "Week" else "Day"
        canvas.drawText(label, MARGIN_PT, cursor.y + 12f, tableHeaderPaint())
        canvas.drawText(if (isWeekly) "Load" else "Points", MARGIN_PT + 150f, cursor.y + 12f, tableHeaderPaint())
        canvas.drawText(if (isWeekly) "Days logged" else "What happened", MARGIN_PT + 260f, cursor.y + 12f, tableHeaderPaint())
        canvas.drawLine(MARGIN_PT, cursor.y + 18f, MARGIN_PT + CONTENT_WIDTH_PT, cursor.y + 18f, headerRulePaint())
        cursor.advance(24f)
    }

    private fun drawTableRow(cursor: PageCursor, row: ReportRow) {
        val canvas = cursor.canvas()
        val baseline = cursor.y + 14f
        when (row) {
            is ReportRow.Day -> {
                canvas.drawText(row.label, MARGIN_PT, baseline, bodyPaint())
                val pointsText = row.points?.let { formatPoints(it) } ?: "-"
                canvas.drawText(pointsText, MARGIN_PT + 150f, baseline, bodyPaint())
                canvas.drawText(truncate(row.summary, 55), MARGIN_PT + 260f, baseline, bodySecondaryPaint())
            }
            is ReportRow.Week -> {
                canvas.drawText(row.label, MARGIN_PT, cursor.y + 26f, bodyPaint())
                val gaugeCenterX = MARGIN_PT + 165f
                val gaugeCenterY = cursor.y + 20f
                drawMiniGauge(canvas, gaugeCenterX, gaugeCenterY, 14f, row.averagePercent)
                val loadText = row.averagePercent?.let { "${it.toInt()}% avg" } ?: "Not logged"
                canvas.drawText(loadText, gaugeCenterX + 22f, cursor.y + 24f, bodyPaint())
                canvas.drawText("${row.daysLogged} of ${row.daysInBucket}", MARGIN_PT + 260f, cursor.y + 26f, bodySecondaryPaint())
            }
        }
        canvas.drawLine(
            MARGIN_PT, cursor.y + (if (row is ReportRow.Week) 42f else 18f),
            MARGIN_PT + CONTENT_WIDTH_PT, cursor.y + (if (row is ReportRow.Week) 42f else 18f),
            dividerPaint()
        )
    }

    /** Same 270-degree arc geometry as RadialLoadGauge.kt, scaled down for a table row and
     * drawn with plain Canvas arcs instead of Compose's DrawScope. */
    private fun drawMiniGauge(canvas: Canvas, cx: Float, cy: Float, radius: Float, percent: Double?) {
        val strokeWidth = 4f
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(oval, GAUGE_START_ANGLE_DEG, GAUGE_SWEEP_MAX_DEG, false, gaugeTrackPaint(strokeWidth))
        if (percent != null) {
            val tier = loadTierFor(percent)
            val sweep = (percent.coerceIn(0.0, 100.0) / 100.0 * GAUGE_SWEEP_MAX_DEG).toFloat()
            canvas.drawArc(oval, GAUGE_START_ANGLE_DEG, sweep, false, gaugeFillPaint(strokeWidth, GAUGE_TIER_COLORS.getValue(tier)))
        }
    }

    private fun drawTopFactorsSection(cursor: PageCursor, topFactors: List<TopFactor>, loggedDayCount: Int, totalDays: Int) {
        val neededHeight = 26f + topFactors.size * 18f + 8f
        cursor.ensureSpace(neededHeight)
        val canvas = cursor.canvas()
        canvas.drawText("Most common factors", MARGIN_PT, cursor.y + 12f, sectionHeaderPaint())
        cursor.advance(22f)
        if (topFactors.isEmpty()) {
            canvas.drawText("Nothing is standing out yet.", MARGIN_PT, cursor.y + 12f, bodySecondaryPaint())
            cursor.advance(18f)
            return
        }
        topFactors.forEach { factor ->
            canvas.drawText(
                "${factor.name}  -  selected on ${factor.selectionCount} of $loggedDayCount logged days ($totalDays-day range)",
                MARGIN_PT, cursor.y + 12f, bodyPaint()
            )
            cursor.advance(18f)
        }
    }

    // ---- Formatting ----

    private fun formatPoints(value: Double): String {
        val rounded = Math.round(value * 10) / 10.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else "%.1f".format(rounded)
    }

    private fun truncate(text: String, maxChars: Int): String =
        if (text.length <= maxChars) text else text.take(maxChars - 1) + "…"

    // ---- Paints (plain android.graphics, not Compose - this file never runs inside a
    // Composable) ----

    private fun titlePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_PRIMARY_COLOR; textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun subtitlePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ACCENT_COLOR; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun sectionHeaderPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_PRIMARY_COLOR; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun tableHeaderPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_SECONDARY_COLOR; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun captionPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_SECONDARY_COLOR; textSize = 9f
    }

    private fun bodyPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_PRIMARY_COLOR; textSize = 10f
    }

    private fun bodySecondaryPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_SECONDARY_COLOR; textSize = 10f
    }

    private fun headerRulePaint() = Paint().apply { color = TEXT_PRIMARY_COLOR; strokeWidth = 1f }

    // Lightweight on borders by design (plan follow-up): a single thin divider between
    // rows, no vertical rules at all.
    private fun dividerPaint() = Paint().apply { color = DIVIDER_COLOR; strokeWidth = 0.75f }

    private fun gridPaint() = Paint().apply { color = TRACK_COLOR; style = Paint.Style.STROKE; strokeWidth = 1f }

    private fun linePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ACCENT_COLOR; style = Paint.Style.STROKE; strokeWidth = 1.5f; strokeCap = Paint.Cap.ROUND
    }

    private fun gaugeTrackPaint(strokeWidth: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TRACK_COLOR; style = Paint.Style.STROKE; this.strokeWidth = strokeWidth; strokeCap = Paint.Cap.ROUND
    }

    private fun gaugeFillPaint(strokeWidth: Float, tierColor: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tierColor; style = Paint.Style.STROKE; this.strokeWidth = strokeWidth; strokeCap = Paint.Cap.ROUND
    }

    /** Tracks the current PdfDocument page/canvas/y-position and starts a fresh page
     * whenever the next block wouldn't fit before the bottom margin. */
    private class PageCursor(private val document: PdfDocument) {
        private var pageNumber = 1
        private var page: PdfDocument.Page = startPage()
        var y: Float = MARGIN_PT
            private set

        fun canvas(): Canvas = page.canvas

        fun ensureSpace(neededHeight: Float) {
            if (wouldOverflow(neededHeight)) startNewPage()
        }

        fun wouldOverflow(neededHeight: Float): Boolean = y + neededHeight > PAGE_HEIGHT_PT - MARGIN_PT

        fun advance(height: Float) {
            y += height
        }

        fun startNewPage() {
            finishCurrentPage()
            pageNumber += 1
            page = startPage()
            y = MARGIN_PT
        }

        fun finishCurrentPage() {
            document.finishPage(page)
        }

        private fun startPage(): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT.toInt(), PAGE_HEIGHT_PT.toInt(), pageNumber).create()
            return document.startPage(info)
        }
    }
}
