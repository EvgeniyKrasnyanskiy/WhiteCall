package com.whitecall.app.util

import android.net.Uri
import android.telecom.Call
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PhoneUtils {

    /**
     * Extracts raw phone number string from Call.Details handle URI.
     */
    fun extractPhoneNumberFromCallDetails(details: Call.Details): String? {
        val handle: Uri? = details.handle
        if (handle == null) return null

        val scheme = handle.scheme
        return if (scheme == "tel" || scheme == "sip") {
            handle.schemeSpecificPart
        } else {
            handle.toString()
        }
    }

    /**
     * Formats timestamp into human-readable date & time (e.g., "16 Aug, 14:32" or "14:32:05").
     */
    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats timestamp into time only (e.g., "14:32").
     */
    fun formatTimeOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Returns the midnight timestamp (start of day) for a given timestamp.
     */
    fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Formats date header for grouped calls list (e.g. "Today", "Yesterday", "15 May").
     */
    fun formatDateGroup(startOfDayMillis: Long, todayStr: String, yesterdayStr: String): String {
        val todayStart = getStartOfDay(System.currentTimeMillis())
        val diffDays = ((todayStart - startOfDayMillis) / (24 * 60 * 60 * 1000)).toInt()
        return when (diffDays) {
            0 -> todayStr
            1 -> yesterdayStr
            else -> {
                val nowYear = Calendar.getInstance().get(Calendar.YEAR)
                val groupYear = Calendar.getInstance().apply { timeInMillis = startOfDayMillis }.get(Calendar.YEAR)
                val pattern = if (nowYear == groupYear) "d MMMM" else "d MMMM yyyy"
                SimpleDateFormat(pattern, Locale.getDefault()).format(Date(startOfDayMillis))
            }
        }
    }
}
