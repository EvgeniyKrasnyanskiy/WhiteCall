package com.whitecall.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.whitecall.app.R
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.ui.MainActivity
import com.whitecall.app.util.PhoneUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WhiteCallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAllWidgets(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_PROTECTION) {
            val app = context.applicationContext as? WhiteCallApplication
            val prefs = app?.preferences
            if (prefs != null) {
                prefs.isProtectionEnabled = !prefs.isProtectionEnabled
            }
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE_PROTECTION = "com.whitecall.app.ACTION_TOGGLE_PROTECTION"

        fun updateAllWidgets(
            context: Context,
            isAlertFlashing: Boolean = false,
            alertCallerDisplay: String? = null
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val thisWidget = ComponentName(context, WhiteCallWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (allWidgetIds == null || allWidgetIds.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                val app = context.applicationContext as? WhiteCallApplication
                val prefs = app?.preferences
                val callRepo = app?.callBlockingRepository

                val isProtectionActive = prefs?.isProtectionCurrentlyActive() ?: false
                val isScheduleEnabled = prefs?.scheduleSettings?.isEnabled ?: false
                val blockedCount = callRepo?.getBlockedTodayCount() ?: 0
                val latestBlocked = callRepo?.getLatestBlockedCall()

                for (widgetId in allWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_white_call)

                    // 1. Status Text and Power Button Icon
                    val statusText: String
                    val statusColor: Int
                    val iconRes: Int
                    val powerColor: Int

                    if (isProtectionActive) {
                        powerColor = ContextCompat.getColor(context, R.color.widget_status_active)
                        if (isScheduleEnabled) {
                            statusText = context.getString(R.string.protection_status_scheduled)
                            statusColor = ContextCompat.getColor(context, R.color.widget_status_scheduled)
                            iconRes = R.drawable.ic_clock
                        } else {
                            statusText = context.getString(R.string.protection_status_active)
                            statusColor = ContextCompat.getColor(context, R.color.widget_status_active)
                            iconRes = R.drawable.ic_shield
                        }
                    } else {
                        powerColor = ContextCompat.getColor(context, R.color.widget_status_inactive)
                        statusText = context.getString(R.string.protection_status_inactive)
                        statusColor = ContextCompat.getColor(context, R.color.widget_status_inactive)
                        iconRes = R.drawable.ic_shield_off
                    }

                    // Left Shield Icon
                    val statusBitmap = drawableToBitmap(context, iconRes, statusColor, 32, 32)
                    if (statusBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_status_icon, statusBitmap)
                    }

                    // Compact Counter Badge
                    views.setTextViewText(R.id.widget_blocked_badge, "🛡 $blockedCount")

                    // Subtitle: Alert or Regular Status
                    if (isAlertFlashing && alertCallerDisplay != null) {
                        val greenColor = ContextCompat.getColor(context, R.color.widget_status_active)
                        val phoneBitmap = drawableToBitmap(context, R.drawable.ic_phone_incoming, greenColor, 28, 28)
                        if (phoneBitmap != null) {
                            views.setImageViewBitmap(R.id.widget_incoming_icon, phoneBitmap)
                        }
                        views.setViewVisibility(R.id.widget_incoming_icon, View.VISIBLE)
                        views.setTextViewText(R.id.widget_status_text, alertCallerDisplay)
                        views.setTextColor(R.id.widget_status_text, greenColor)
                    } else if (latestBlocked != null && blockedCount > 0) {
                        val callerDisplay = latestBlocked.callerName ?: latestBlocked.phoneNumber
                        val timeDisplay = PhoneUtils.formatTimeOnly(latestBlocked.timestamp)
                        val greenColor = ContextCompat.getColor(context, R.color.widget_status_active)
                        val phoneBitmap = drawableToBitmap(context, R.drawable.ic_phone_incoming, greenColor, 28, 28)
                        if (phoneBitmap != null) {
                            views.setImageViewBitmap(R.id.widget_incoming_icon, phoneBitmap)
                        }
                        views.setViewVisibility(R.id.widget_incoming_icon, View.VISIBLE)
                        views.setTextViewText(R.id.widget_status_text, "$callerDisplay • $timeDisplay")
                        views.setTextColor(R.id.widget_status_text, ContextCompat.getColor(context, R.color.widget_text_secondary))
                    } else {
                        views.setViewVisibility(R.id.widget_incoming_icon, View.GONE)
                        views.setTextViewText(R.id.widget_status_text, statusText)
                        views.setTextColor(R.id.widget_status_text, statusColor)
                    }

                    // Right Power Button (Classic ⏻)
                    val powerBitmap = drawableToBitmap(context, R.drawable.ic_power, powerColor, 48, 48)
                    if (powerBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_toggle_button, powerBitmap)
                    }

                    // PendingIntent to launch MainActivity on root tap
                    val mainIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val mainPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

                    // PendingIntent to toggle protection status
                    val toggleIntent = Intent(context, WhiteCallWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE_PROTECTION
                    }
                    val togglePendingIntent = PendingIntent.getBroadcast(
                        context,
                        1,
                        toggleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_toggle_button, togglePendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }

        /**
         * Flashes the green incoming call icon on the widget for several cycles when a call is blocked.
         */
        fun flashIncomingCallAlert(context: Context, callerDisplay: String) {
            CoroutineScope(Dispatchers.IO).launch {
                for (i in 0 until 4) {
                    updateAllWidgets(context, isAlertFlashing = (i % 2 == 0), alertCallerDisplay = callerDisplay)
                    delay(400)
                }
                updateAllWidgets(context, isAlertFlashing = false)
            }
        }

        private fun drawableToBitmap(
            context: Context,
            @DrawableRes drawableId: Int,
            @ColorInt tintColor: Int? = null,
            widthDp: Int = 48,
            heightDp: Int = 48
        ): Bitmap? {
            val drawable = ContextCompat.getDrawable(context, drawableId)?.mutate() ?: return null
            if (tintColor != null) {
                drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
            }
            val density = context.resources.displayMetrics.density
            val widthPx = (widthDp * density).toInt().coerceAtLeast(1)
            val heightPx = (heightDp * density).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
    }
}
