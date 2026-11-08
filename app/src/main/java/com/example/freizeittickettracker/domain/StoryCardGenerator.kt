package com.example.freizeittickettracker.domain

import android.content.Context
import android.graphics.*
import androidx.core.content.FileProvider
import com.example.freizeittickettracker.data.VisitEntity
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class StorySummaryData(
    val season: String,
    val totalVisits: Int,
    val totalSaved: Double,
    val breakEvenReached: Boolean,
    val topVenues: List<Pair<String, Int>>,
    val uniqueVenuesCount: Int
)

object StoryCardGenerator {

    private const val WIDTH = 1080
    private const val HEIGHT = 1920

    private val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.GERMAN))

    enum class StoryStyle(val title: String) {
        STATS_CLASSIC("Alpine Stats"),
        TOP_SPOTS("Top Gebiete"),
        MINIMAL_DARK("Minimal Night")
    }

    fun generateStoryBitmap(context: Context, data: StorySummaryData, style: StoryStyle): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (style) {
            StoryStyle.STATS_CLASSIC -> drawClassicStyle(canvas, data)
            StoryStyle.TOP_SPOTS -> drawTopSpotsStyle(canvas, data)
            StoryStyle.MINIMAL_DARK -> drawMinimalStyle(canvas, data)
        }

        return bitmap
    }

    private fun drawClassicStyle(canvas: Canvas, data: StorySummaryData) {
        // Hintergrund: Eleganter tiefblauer Farbverlauf
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, HEIGHT.toFloat(),
                Color.parseColor("#0F172A"),
                Color.parseColor("#0284C7"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // Header
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FTT WRAPPED \u2022 ${data.season}", WIDTH / 2f, 220f, brandPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 68f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Deine Saisonbilanz", WIDTH / 2f, 310f, titlePaint)

        // Card Container
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            alpha = 230
        }
        val cardRect = RectF(80f, 400f, WIDTH - 80f, 1500f)
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

        // KPI 1: Besuche
        drawKpiBlock(canvas, 550f, "${data.totalVisits}", "SKITAGE & BESUCHE", "#38BDF8")

        // Trennlinie
        val divPaint = Paint().apply { color = Color.parseColor("#334155"); strokeWidth = 3f }
        canvas.drawLine(160f, 680f, WIDTH - 160f, 680f, divPaint)

        // KPI 2: Ersparnis / Break-Even
        val savedText = "${if (data.totalSaved >= 0) "+" else ""}${df.format(data.totalSaved)} €"
        val savedSub = if (data.breakEvenReached) "\u2705 BREAK-EVEN ERREICHT" else "OFFEN ZUM BREAK-EVEN"
        val savedColor = if (data.breakEvenReached) "#10B981" else "#F59E0B"
        drawKpiBlock(canvas, 800f, savedText, savedSub, savedColor)

        canvas.drawLine(160f, 930f, WIDTH - 160f, 930f, divPaint)

        // Top Spot
        val topSpotName = data.topVenues.firstOrNull()?.first ?: "Keine Besuche"
        val topSpotCount = data.topVenues.firstOrNull()?.second ?: 0
        drawKpiBlock(canvas, 1050f, topSpotName, "LIEBLINGSGEBIET ($topSpotCount BESUCHE)", "#FFFFFF")

        canvas.drawLine(160f, 1180f, WIDTH - 160f, 1180f, divPaint)

        // Verschiedene Gebiete
        drawKpiBlock(canvas, 1300f, "${data.uniqueVenuesCount}", "VERSCHIEDENE GEBIETE ERKUNDET", "#38BDF8")

        // Footer Watermark
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Getrackt mit Freizeitticket Tracker \u2744\uFE0F", WIDTH / 2f, 1720f, footerPaint)
    }

    private fun drawTopSpotsStyle(canvas: Canvas, data: StorySummaryData) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, HEIGHT.toFloat(),
                Color.parseColor("#1E1B4B"),
                Color.parseColor("#4338CA"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A5B4FC")
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("MEINE TOP GEBIETE", WIDTH / 2f, 220f, brandPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Saison ${data.season}", WIDTH / 2f, 310f, subPaint)

        // Podium Cards
        var startY = 480f
        val ranks = listOf("\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49")
        val top3 = data.topVenues.take(3)

        top3.forEachIndexed { idx, pair ->
            val rect = RectF(90f, startY, WIDTH - 90f, startY + 220f)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#312E81"); alpha = 240 }
            canvas.drawRoundRect(rect, 36f, 36f, p)

            val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 70f; textAlign = Paint.Align.LEFT }
            canvas.drawText(ranks.getOrElse(idx) { "#${idx + 1}" }, 130f, startY + 140f, rankPaint)

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 50f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val shortName = if (pair.first.length > 18) pair.first.take(17) + "…" else pair.first
            canvas.drawText(shortName, 260f, startY + 115f, namePaint)

            val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#C7D2FE")
                textSize = 34f
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("${pair.second}x besucht", 260f, startY + 175f, countPaint)

            startY += 260f
        }

        // Summary Pill unten
        val pillRect = RectF(120f, 1360f, WIDTH - 120f, 1540f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4F46E5") }
        canvas.drawRoundRect(pillRect, 40f, 40f, pillPaint)

        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${data.totalVisits} Tage unterwegs \u2022 ${data.uniqueVenuesCount} Spots", WIDTH / 2f, 1460f, totalPaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C7D2FE")
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Freizeitticket Tirol Tracker \u2744\uFE0F", WIDTH / 2f, 1720f, footerPaint)
    }

    private fun drawMinimalStyle(canvas: Canvas, data: StorySummaryData) {
        // Deep Pitch Black
        canvas.drawColor(Color.parseColor("#09090B"))

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#71717A")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.2f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FREIZEITTICKET STATS", WIDTH / 2f, 260f, brandPaint)

        // Riesige Tage-Zahl
        val bigNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 190f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${data.totalVisits}", WIDTH / 2f, 650f, bigNumPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A1A1AA")
            textSize = 42f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Tage im Schnee & am Berg", WIDTH / 2f, 730f, labelPaint)

        // Minimalistisches Ersparnis-Highlight
        val savedText = "${if (data.totalSaved >= 0) "+" else ""}${df.format(data.totalSaved)} €"
        val savedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (data.breakEvenReached) Color.parseColor("#22C55E") else Color.parseColor("#F59E0B")
            textSize = 96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(savedText, WIDTH / 2f, 1080f, savedPaint)

        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#71717A")
            textSize = 36f
            letterSpacing = 0.15f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(if (data.breakEvenReached) "NETTO-ERSPARNIS" else "NOCH OFFEN", WIDTH / 2f, 1150f, statusPaint)

        val seasonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#52525B")
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Saison ${data.season}", WIDTH / 2f, 1720f, seasonPaint)
    }

    private fun drawKpiBlock(canvas: Canvas, baselineY: Float, value: String, label: String, valueColor: String) {
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(valueColor)
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(value, WIDTH / 2f, baselineY, valPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 28f
            letterSpacing = 0.1f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, WIDTH / 2f, baselineY + 45f, subPaint)
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): android.net.Uri {
        val shareDir = File(context.cacheDir, "story_shares")
        if (!shareDir.exists()) shareDir.mkdirs()

        val file = File(shareDir, "$fileName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}