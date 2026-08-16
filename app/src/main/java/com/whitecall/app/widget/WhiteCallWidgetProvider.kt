package com.whitecall.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.whitecall.app.R
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.ui.MainActivity
import com.whitecall.app.util.PhoneUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WhiteCallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
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

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, WhiteCallWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

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

                    // 1. Status Text and Color
                    val statusText: String
                    val statusColor: Int
                    val iconRes: Int

                    if (isProtectionActive) {
                        if (isScheduleEnabled) {
                            statusText = context.getString(R.string.protection_status_scheduled)
                            statusColor = context.getColor(R.color.widget_status_scheduled)
                            iconRes = R.drawable.ic_clock
                        } else {
                            statusText = context.getString(R.string.protection_status_active)
                            statusColor = context.getColor(R.color.widget_status_active)
                            iconRes = R.drawable.ic_shield
                        }
                    } else {
                        statusText = context.getString(R.string.protection_status_inactive)
                        statusColor = context.getColor(R.color.widget_status_inactive)
                        iconRes = R.drawable.ic_shield_off
                    }

                    views.setTextViewText(R.id.widget_status_text, statusText)
                    views.setTextColor(R.id.widget_status_text, statusColor)
                    views.setImageViewResource(R.id.widget_status_icon, iconRes)

                    // 2. Blocked Count
                    val countText = if (blockedCount > 0) {
                        context.getString(R.string.widget_blocked_today, blockedCount)
                    } else {
                        context.getString(R.string.widget_no_blocked_today)
                    }
                    views.setTextViewText(R.id.widget_blocked_count, countText)

                    // 3. Last Blocked Caller Info
                    if (latestBlocked != null) {
                        val callerDisplay = latestBlocked.callerName ?: latestBlocked.phoneNumber
                        val timeDisplay = PhoneUtils.formatTimeOnly(latestBlocked.timestamp)
                        views.setTextViewText(
                            R.id.widget_last_blocked_caller,
                            context.getString(R.string.widget_last_blocked, callerDisplay, timeDisplay)
                        )
                    } else {
                        views.setTextViewText(R.id.widget_last_blocked_caller, "")
                    }

                    // 4. PendingIntent to launch MainActivity on root tap
                    val mainIntent = Intent(context, MainActivity::class.java)
                    val mainPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

                    // 5. PendingIntent to toggle protection status
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

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            updateAllWidgets(context)
        }
    }
}
