package com.whitecall.app

import com.whitecall.app.domain.model.ScheduleSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleSettingsTest {

    @Test
    fun isScheduleActive_disabled_returnsFalse() {
        val settings = ScheduleSettings(isEnabled = false)
        val cal = Calendar.getInstance()
        assertFalse(settings.isScheduleActive(cal))
    }

    @Test
    fun isScheduleActive_daytimeSchedule_activeInsideWindow() {
        val settings = ScheduleSettings(
            isEnabled = true,
            startHour = 9,
            startMinute = 0,
            endHour = 18,
            endMinute = 0,
            activeDays = setOf(Calendar.MONDAY)
        )

        val activeCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(settings.isScheduleActive(activeCal))

        val outsideCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(settings.isScheduleActive(outsideCal))

        val wrongDayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        assertFalse(settings.isScheduleActive(wrongDayCal))
    }

    @Test
    fun isScheduleActive_overnightSchedule_activeInsideWindow() {
        val settings = ScheduleSettings(
            isEnabled = true,
            startHour = 22,
            startMinute = 0,
            endHour = 7,
            endMinute = 0,
            activeDays = setOf(Calendar.MONDAY, Calendar.TUESDAY)
        )

        // 23:30 on Monday -> Active
        val lateNightCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(settings.isScheduleActive(lateNightCal))

        // 05:15 on Monday -> Active
        val earlyMorningCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 15)
        }
        assertTrue(settings.isScheduleActive(earlyMorningCal))

        // 12:00 on Monday -> Inactive
        val middayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(settings.isScheduleActive(middayCal))
    }
}
