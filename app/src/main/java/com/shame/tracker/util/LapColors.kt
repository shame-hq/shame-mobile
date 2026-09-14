package com.shame.tracker.util

import android.graphics.Color

/**
 * Canonical lap colour palette — cycles when lap count > palette size.
 * Matches the lap_1..lap_6 colours defined in colors.xml.
 */
object LapColors {

    private val PALETTE = intArrayOf(
        Color.parseColor("#3B82F6"), // lap_1 – blue
        Color.parseColor("#10B981"), // lap_2 – emerald
        Color.parseColor("#F59E0B"), // lap_3 – amber
        Color.parseColor("#EF4444"), // lap_4 – red
        Color.parseColor("#8B5CF6"), // lap_5 – violet
        Color.parseColor("#EC4899"), // lap_6 – pink
    )

    /** Zero-based lap index → ARGB colour int. */
    fun forIndex(index: Int): Int = PALETTE[index % PALETTE.size]

    /** One-based lap number → ARGB colour int. */
    fun forLapNumber(lapNumber: Int): Int = forIndex(lapNumber - 1)
}
