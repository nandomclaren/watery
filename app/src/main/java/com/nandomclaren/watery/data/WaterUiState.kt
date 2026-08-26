package com.nandomclaren.watery.data

import kotlin.math.ceil
import kotlin.math.roundToInt

data class WaterUiState(
    val goalMl: Int = 2000,
    val glassMl: Int = 250,
    val drunkMl: Int = 0,
) {
    val goalGlasses: Int
        get() = ceil(goalMl.toDouble() / glassMl.coerceAtLeast(1)).toInt().coerceAtLeast(1)

    val drunkGlasses: Int
        get() = (drunkMl.toDouble() / glassMl.coerceAtLeast(1)).roundToInt().coerceAtLeast(0)

    val remainingGlasses: Int
        get() = (goalGlasses - drunkGlasses).coerceAtLeast(0)

    val progressFraction: Float
        get() = if (goalMl <= 0) 0f else (drunkMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)

    val drunkLiters: Double
        get() = drunkMl / 1000.0
}
