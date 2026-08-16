package com.whitecall.app.service

import android.content.Context
import android.telephony.TelephonyManager
import com.whitecall.app.widget.WhiteCallWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object CallStateMonitor {

    private var activeCallJob: Job? = null

    fun onCallBlocked(context: Context, callerDisplay: String) {
        // Cancel any previous alert job
        activeCallJob?.cancel()

        activeCallJob = CoroutineScope(Dispatchers.IO).launch {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val appContext = context.applicationContext

            // Loop while call is active (up to 40 seconds max carrier ring timeout)
            for (step in 0 until 80) {
                val isPulse = (step % 2 == 0)

                // Update widgets with flashing handset icon and caller's name
                WhiteCallWidgetProvider.updateAllWidgets(
                    context = appContext,
                    isAlertFlashing = isPulse,
                    alertCallerDisplay = callerDisplay
                )

                delay(500)

                // Check telephony state after 2 seconds
                if (step >= 4 && telephonyManager != null) {
                    try {
                        @Suppress("DEPRECATION")
                        val state = telephonyManager.callState
                        if (state == TelephonyManager.CALL_STATE_IDLE) {
                            // Caller stopped dialing / hung up
                            break
                        }
                    } catch (_: Exception) {}
                }
            }

            // Immediately clear alert and restore normal block counter
            WhiteCallWidgetProvider.updateAllWidgets(
                context = appContext,
                isAlertFlashing = false,
                alertCallerDisplay = null
            )
        }
    }
}
