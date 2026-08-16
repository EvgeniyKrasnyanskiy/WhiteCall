package com.whitecall.app.util

import android.net.Uri
import android.telecom.Call
import java.text.SimpleDateFormat
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
}
