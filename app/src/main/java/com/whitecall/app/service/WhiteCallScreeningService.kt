package com.whitecall.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.whitecall.app.R
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.util.PhoneUtils
import com.whitecall.app.widget.WhiteCallWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WhiteCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val app = applicationContext as? WhiteCallApplication
        if (app == null) {
            // Safety fallback: allow call if application instance is not available
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Only filter incoming calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val rawPhoneNumber = PhoneUtils.extractPhoneNumberFromCallDetails(callDetails)

        serviceScope.launch {
            try {
                val result = app.shouldBlockCallUseCase.evaluateCall(
                    context = this@WhiteCallScreeningService,
                    rawIncomingNumber = rawPhoneNumber
                )

                if (result.shouldBlock) {
                    val blockMode = app.preferences.blockMode
                    val response = if (blockMode == com.whitecall.app.data.preferences.AppPreferences.BLOCK_MODE_SILENCE) {
                        CallResponse.Builder()
                            .setDisallowCall(false)
                            .setSilenceCall(true)
                            .setSkipCallLog(true)
                            .setSkipNotification(true)
                            .build()
                    } else {
                        CallResponse.Builder()
                            .setDisallowCall(true)
                            .setRejectCall(true)
                            .setSkipCallLog(false)
                            .setSkipNotification(true)
                            .build()
                    }
                    respondToCall(callDetails, response)

                    // Log blocked call in database
                    val displayPhone = rawPhoneNumber ?: getString(R.string.unknown_caller)
                    app.callBlockingRepository.recordBlockedCall(
                        phoneNumber = displayPhone,
                        callerName = result.callerName,
                        reason = result.reason
                    )

                    // Flash green incoming handset alert on widget
                    val alertName = result.callerName ?: displayPhone
                    WhiteCallWidgetProvider.flashIncomingCallAlert(this@WhiteCallScreeningService, alertName)
                } else {
                    val response = CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .setSkipNotification(false)
                        .setSkipCallLog(false)
                        .build()

                    respondToCall(callDetails, response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // On unexpected exception, do not drop critical emergency/normal calls
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }
}
