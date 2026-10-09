package com.carlos.karoopantallas.render

import android.graphics.Color

/** Colores de cada versión de la pantalla (1B oscura, 1C clara), sacados de las maquetas. */
class Palette(
    val bg: Int,
    val number: Int,
    val label: Int,
    val unit: Int,
    val secondary: Int,
    val neutralStrip: Int,
    /** 7 zonas de potencia (modelo Coggan del Karoo). */
    private val power7: List<Int>,
    /** 5 zonas de pulso. */
    private val hr5: List<Int>,
    /** Pendiente: <1 %, <3 %, <6 %, <9 %, <12 %, resto. */
    private val grades: List<Int>,
) {
    fun powerZone(index: Int, count: Int): Int = pick(if (count == 7) power7 else power7.drop(1), index)
    fun hrZone(index: Int, count: Int): Int = pick(if (count <= 5) hr5 else power7, index)

    fun grade(g: Double?): Int = when {
        g == null || g < 1 -> grades[0]
        g < 3 -> grades[1]
        g < 6 -> grades[2]
        g < 9 -> grades[3]
        g < 12 -> grades[4]
        else -> grades[5]
    }

    private fun pick(list: List<Int>, i: Int) = list[i.coerceIn(0, list.lastIndex)]

    companion object {
        private fun c(hex: String) = Color.parseColor(hex)

        /** 1B · fondo negro */
        val DARK = Palette(
            bg = c("#000000"),
            number = c("#FFFFFF"),
            label = c("#E6EAEE"),
            unit = c("#AEB8C4"),
            secondary = c("#E6EAEE"),
            neutralStrip = c("#3A4552"),
            power7 = listOf("#8B97A6", "#6FA8FF", "#35D0BA", "#F4E04D", "#FF8A1F", "#FF5A4E", "#C86BFF").map(::c),
            hr5 = listOf("#6FA8FF", "#35D0BA", "#F4E04D", "#FF8A1F", "#FF5A4E").map(::c),
            grades = listOf("#FFFFFF", "#35D0BA", "#F4E04D", "#FF8A1F", "#FF5A4E", "#C86BFF").map(::c),
        )

        /** 1C · fondo claro, con la versión oscura de cada color para que se lea al sol */
        val LIGHT = Palette(
            bg = c("#F4F2EC"),
            number = c("#0C0F13"),
            label = c("#1A2028"),
            unit = c("#4A525C"),
            secondary = c("#1A2028"),
            neutralStrip = c("#B9B5AA"),
            power7 = listOf("#5B6673", "#1F5FD1", "#0B8A78", "#A07D00", "#C85A00", "#C62828", "#7B2FBF").map(::c),
            hr5 = listOf("#1F5FD1", "#0B8A78", "#A07D00", "#C85A00", "#C62828").map(::c),
            grades = listOf("#0C0F13", "#0B8A78", "#A07D00", "#C85A00", "#C62828", "#7B2FBF").map(::c),
        )
    }
}
