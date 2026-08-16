package com.whitecall.app.domain.model

import java.util.Calendar

data class ScheduleSettings(
    val isEnabled: Boolean = false,
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0,
    // Days of week active: 1 = Sunday, 2 = Monday, ..., 7 = Saturday (java.util.Calendar constants)
    val activeDays: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    )
) {
    /**
     * Checks whether the given calendar time falls within the active schedule window.
     */
    fun isScheduleActive(calendar: Calendar = Calendar.getInstance()): Boolean {
        if (!isEnabled) return false

        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        if (!activeDays.contains(currentDay)) {
            return false
        }

        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            // Same day range, e.g. 09:00 to 18:00
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight range, e.g. 22:00 to 07:00
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
