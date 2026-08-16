package com.whitecall.app.service

import android.content.Context
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
            val appContext = context.applicationContext

            // Flash green handset and show caller for 10 full seconds (20 pulses x 500ms)
            for (step in 0 until 20) {
                val isPulse = (step % 2 == 0)

                WhiteCallWidgetProvider.updateAllWidgets(
                    context = appContext,
                    isAlertFlashing = isPulse,
                    alertCallerDisplay = callerDisplay
                )

                delay(500)
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
