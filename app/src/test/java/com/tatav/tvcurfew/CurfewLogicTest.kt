package com.tatav.tvcurfew

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Proves the 9 PM -> 10 AM gate for both the "lock" and "allow" branches. */
class CurfewLogicTest {

    private fun at(hour: Int, minute: Int = 0): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    // --- inside curfew: TV must be forced off ---
    @Test fun ninePM_isCurfew()          = assertTrue(CurfewLogic.isCurfew(at(21, 0)))
    @Test fun elevenPM_isCurfew()        = assertTrue(CurfewLogic.isCurfew(at(23, 3)))
    @Test fun midnight_isCurfew()        = assertTrue(CurfewLogic.isCurfew(at(0, 0)))
    @Test fun threeAM_isCurfew()         = assertTrue(CurfewLogic.isCurfew(at(3, 0)))
    @Test fun justBeforeTenAM_isCurfew() = assertTrue(CurfewLogic.isCurfew(at(9, 59)))

    // --- outside curfew: TV must be left alone ---
    @Test fun tenAM_isAllowed()   = assertFalse(CurfewLogic.isCurfew(at(10, 0)))
    @Test fun noon_isAllowed()    = assertFalse(CurfewLogic.isCurfew(at(12, 0)))
    @Test fun twoPM_isAllowed()   = assertFalse(CurfewLogic.isCurfew(at(14, 0)))
    @Test fun eightPM_isAllowed() = assertFalse(CurfewLogic.isCurfew(at(20, 59)))
}
