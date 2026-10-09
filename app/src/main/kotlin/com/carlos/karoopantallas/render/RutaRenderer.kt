package com.carlos.karoopantallas.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.min

/** Valores ya formateados que muestra la pantalla 1B / 1C. */
data class RutaData(
    val speed: String,
    val speedUnit: String,
    val avgSpeed: String,
    val power: String,
    val powerZone: Int?,
    val powerZoneCount: Int,
    val normPower: String,
    val hr: String,
    val hrZone: Int?,
    val hrZoneCount: Int,
    val grade: Double?,
    val gradeText: String,
    val distance: String,
    val distUnit: String,
    val elevation: String,
    val elevUnit: String,
)

/**
 * Dibuja la pantalla completa en un Bitmap, copiando la maqueta 1B/1C (diseño de 480 × 620).
 * Se dibuja a mano (en lugar de con vistas de Android) para poder usar Barlow Condensed,
 * cifras de ancho fijo y estirar en vertical las cifras grandes, igual que en el diseño.
 */
class RutaRenderer(context: Context) {

    private val condensed: Typeface = Typeface.createFromAsset(context.assets, "fonts/BarlowCondensed-Bold.ttf")
    private val barlow: Typeface = Typeface.createFromAsset(context.assets, "fonts/Barlow-Bold.ttf")

    private val numPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = condensed
        fontFeatureSettings = "'tnum'" // cifras de ancho fijo: no "bailan" al cambiar
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply { typeface = barlow }
    private val fill = Paint()

    companion object {
        const val W = 480f
        const val H = 620f

        /** Proporción de la altura de las cifras respecto al tamaño de letra (Barlow: 0,7). */
        private const val CAP = 0.7f
    }

    fun render(widthPx: Int, heightPx: Int, d: RutaData, p: Palette): Bitmap {
        val bmp = Bitmap.createBitmap(max(1, widthPx), max(1, heightPx), Bitmap.Config.RGB_565)
        val c = Canvas(bmp)
        c.drawColor(p.bg)
        // Todo se dibuja en coordenadas de diseño (480 × 620) y se escala al tamaño real del campo
        c.scale(widthPx / W, heightPx / H)

        drawSpeed(c, d, p)
        drawPower(c, d, p)
        drawHrAndGrade(c, d, p)
        drawDistanceAndElevation(c, d, p)
        return bmp
    }

    // ---------------- Bloques ----------------

    /** Velocidad (0–165): cifra estirada + km/h; media abajo a la derecha. */
    private fun drawSpeed(c: Canvas, d: RutaData, p: Palette) {
        val base = 157f
        val right = 462f
        // Media
        val mediaW = max(width(numPaint, d.avgSpeed, 72f), width(textPaint, "MEDIA", 26f))
        text(c, numPaint, d.avgSpeed, 72f, p.secondary, right, 155f, alignRight = true)
        text(c, textPaint, "MEDIA", 26f, p.unit, right, 155f - 72f * CAP - 10f, alignRight = true)
        // Velocidad
        val unitW = width(textPaint, d.speedUnit, 24f)
        val maxW = right - mediaW - 16f - 18f - unitW - 6f
        val size = fit(numPaint, d.speed, 148f, maxW)
        val w = width(numPaint, d.speed, size)
        stretched(c, d.speed, size, p.number, 18f, base, 1.15f)
        text(c, textPaint, d.speedUnit, 24f, p.unit, 18f + w + 6f, base)
    }

    /** Potencia (165–320): franja de zona, cifra estirada, Zx arriba y W abajo; NORM a la derecha. */
    private fun drawPower(c: Canvas, d: RutaData, p: Palette) {
        val top = 165f
        val bottom = 320f
        val zoneColor = d.powerZone?.let { p.powerZone(it, d.powerZoneCount) } ?: p.neutralStrip
        rect(c, 0f, top, 14f, bottom, zoneColor)

        val base = 312f
        val right = 462f
        // NORM
        val normW = max(width(numPaint, d.normPower, 61f), width(textPaint, "NORM", 26f))
        text(c, numPaint, d.normPower, 61f, p.secondary, right, 310f, alignRight = true)
        text(c, textPaint, "NORM", 26f, p.unit, right, 310f - 61f * CAP - 10f, alignRight = true)
        // Cifra + columna Zx / W
        val zoneLabel = d.powerZone?.let { "Z${it + 1}" }
        val colW = max(width(textPaint, zoneLabel ?: "Z0", 48f), width(textPaint, "W", 36f))
        val x = 32f
        val maxW = right - normW - 16f - x - colW - 10f
        val size = fit(numPaint, d.power, 136f, maxW)
        val w = width(numPaint, d.power, size)
        val numColor = if (d.powerZone != null) zoneColor else p.number
        stretched(c, d.power, size, numColor, x, base, 1.12f)
        val numTop = base - size * CAP * 1.12f
        if (zoneLabel != null) text(c, textPaint, zoneLabel, 48f, zoneColor, x + w + 10f, numTop + 48f * CAP)
        text(c, textPaint, "W", 36f, p.unit, x + w + 10f, base - 2f)
    }

    /** Pulso (izquierda, con franja de zona) y pendiente (derecha, color por inclinación): 320–470. */
    private fun drawHrAndGrade(c: Canvas, d: RutaData, p: Palette) {
        val top = 320f
        val bottom = 470f
        val labelBase = top + 25f
        val valueBase = 352f + (bottom - 352f + 96f * CAP) / 2f

        // Pulso
        val hrColor = d.hrZone?.let { p.hrZone(it, d.hrZoneCount) } ?: p.neutralStrip
        rect(c, 0f, top, 14f, bottom, hrColor)
        text(c, textPaint, "PULSO", 22f, p.label, 32f, labelBase)
        d.hrZone?.let { text(c, textPaint, "Z${it + 1}", 26f, hrColor, 226f, labelBase + 1f, alignRight = true) }
        val hrSize = fit(numPaint, d.hr, 96f, 226f - 32f)
        text(c, numPaint, d.hr, hrSize, if (d.hrZone != null) hrColor else p.number, 32f, valueBase)

        // Pendiente
        val gColor = p.grade(d.grade)
        text(c, textPaint, "PENDIENTE", 22f, p.label, 258f, labelBase)
        val pctW = width(numPaint, "%", 48f)
        val gSize = fit(numPaint, d.gradeText, 96f, 462f - 258f - pctW - 4f)
        val gW = width(numPaint, d.gradeText, gSize)
        text(c, numPaint, d.gradeText, gSize, gColor, 258f, valueBase)
        if (d.grade != null) text(c, numPaint, "%", 48f, gColor, 258f + gW + 4f, valueBase)
    }

    /** Distancia y desnivel acumulado (470–620). */
    private fun drawDistanceAndElevation(c: Canvas, d: RutaData, p: Palette) {
        val labelBase = 470f + 25f
        val valueBase = 503f + (620f - 503f + 88f * CAP) / 2f

        // Distancia (unidad en el rótulo)
        val lbl = "DISTANCIA "
        text(c, textPaint, lbl, 22f, p.label, 18f, labelBase)
        text(c, textPaint, d.distUnit, 22f, p.unit, 18f + width(textPaint, lbl, 22f), labelBase)
        val dSize = fit(numPaint, d.distance, 88f, 240f - 18f - 10f)
        text(c, numPaint, d.distance, dSize, p.number, 18f, valueBase)

        // Desnivel (unidad tras la cifra)
        text(c, textPaint, "DESNIVEL +", 22f, p.label, 258f, labelBase)
        val uW = width(textPaint, d.elevUnit, 24f)
        val eSize = fit(numPaint, d.elevation, 88f, 462f - 258f - uW - 6f)
        val eW = width(numPaint, d.elevation, eSize)
        text(c, numPaint, d.elevation, eSize, p.number, 258f, valueBase)
        text(c, textPaint, d.elevUnit, 24f, p.unit, 258f + eW + 6f, valueBase)
    }

    // ---------------- Utilidades ----------------

    private fun text(c: Canvas, paint: Paint, s: String, size: Float, color: Int, x: Float, baseline: Float, alignRight: Boolean = false) {
        paint.textSize = size
        paint.color = color
        val px = if (alignRight) x - paint.measureText(s) else x
        c.drawText(s, px, baseline, paint)
    }

    /** Cifra estirada en vertical desde su línea base (como en la maqueta). */
    private fun stretched(c: Canvas, s: String, size: Float, color: Int, x: Float, baseline: Float, scaleY: Float) {
        c.save()
        c.scale(1f, scaleY, x, baseline)
        text(c, numPaint, s, size, color, x, baseline)
        c.restore()
    }

    private fun width(paint: Paint, s: String, size: Float): Float {
        paint.textSize = size
        return paint.measureText(s)
    }

    /** Reduce el tamaño si la cifra no cabe en el ancho disponible (p. ej. velocidades de 3 cifras). */
    private fun fit(paint: Paint, s: String, desired: Float, maxWidth: Float): Float {
        val w = width(paint, s, desired)
        return if (w <= maxWidth || w <= 0f) desired else desired * min(1f, maxWidth / w)
    }

    private fun rect(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) {
        fill.color = color
        c.drawRect(l, t, r, b, fill)
    }
}
