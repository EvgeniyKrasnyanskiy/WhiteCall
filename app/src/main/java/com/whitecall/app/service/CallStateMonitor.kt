package com.whitecall.app.service

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.whitecall.app.widget.WhiteCallWidgetProvider

object CallStateMonitor {

    private var isListening = false
    private var currentRingingCaller: String? = null

    fun startListening(context: Context) {
        if (isListening) return
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallState(context, state)
                }
            }
            try {
                telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
                isListening = true
            } catch (_: Exception) {}
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallState(context, state)
                }
            }
            try {
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                isListening = true
            } catch (_: Exception) {}
        }
    }

    fun onCallBlocked(context: Context, callerDisplay: String) {
        currentRingingCaller = callerDisplay
        startListening(context)
        WhiteCallWidgetProvider.updateAllWidgets(
            context = context,
            isAlertFlashing = true,
            alertCallerDisplay = callerDisplay
        )
    }

    private fun handleCallState(context: Context, state: Int) {
        if (state == TelephonyManager.CALL_STATE_IDLE) {
            // Call ended (caller hung up / ringing stopped)
            if (currentRingingCaller != null) {
                currentRingingCaller = null
                WhiteCallWidgetProvider.updateAllWidgets(
                    context = context,
                    isAlertFlashing = false,
                    alertCallerDisplay = null
                )
            }
        }
    }
}
