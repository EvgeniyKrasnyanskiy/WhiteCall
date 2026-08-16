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
import kotlinx.coroutines.launch

class WhiteCallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
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

        fun updateAllWidgets(context: Context) {
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

                    // 1. Status Text and Color
                    val statusText: String
                    val statusColor: Int
                    val iconRes: Int

                    if (isProtectionActive) {
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
                        statusText = context.getString(R.string.protection_status_inactive)
                        statusColor = ContextCompat.getColor(context, R.color.widget_status_inactive)
                        iconRes = R.drawable.ic_shield_off
                    }

                    views.setTextViewText(R.id.widget_status_text, statusText)
                    views.setTextColor(R.id.widget_status_text, statusColor)

                    // Set status icon via safe Bitmap
                    val statusBitmap = drawableToBitmap(context, iconRes, statusColor, 48, 48)
                    if (statusBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_status_icon, statusBitmap)
                    }

                    // Set toggle button icon via safe Bitmap
                    val toggleColor = ContextCompat.getColor(context, R.color.widget_accent)
                    val refreshBitmap = drawableToBitmap(context, R.drawable.ic_refresh, toggleColor, 48, 48)
                    if (refreshBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_toggle_button, refreshBitmap)
                    }

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
