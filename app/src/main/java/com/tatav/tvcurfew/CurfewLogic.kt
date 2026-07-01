package com.tatav.tvcurfew

import java.util.Calendar

/**
 * The curfew window: 9:00 PM (inclusive) to 10:00 AM (exclusive), spanning midnight.
 * Anytime the local clock is in [21:00, 24:00) OR [00:00, 10:00) the TV must be off.
 */
object CurfewLogic {
    const val START_HOUR = 21 // 9 PM
    const val END_HOUR = 10   // 10 AM

    fun isCurfew(cal: Calendar): Boolean {
        val h = cal.get(Calendar.HOUR_OF_DAY)
        return h >= START_HOUR || h < END_HOUR
    }

    fun isCurfewNow(): Boolean = isCurfew(Calendar.getInstance())
}
