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
import android.os.Build
import android.os.Bundle
import android.util.SizeF
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
        newOptions: Bundle
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
                val whiteListRepo = app?.whiteListRepository

                val isProtectionActive = prefs?.isProtectionCurrentlyActive() ?: false
                val isScheduleEnabled = prefs?.scheduleSettings?.isEnabled ?: false
                val blockedToday = callRepo?.getBlockedTodayCount() ?: 0
                val blockedWeek = callRepo?.getBlockedWeekCount() ?: 0
                val blockedMonth = callRepo?.getBlockedMonthCount() ?: 0
                val whiteListCount = whiteListRepo?.getWhiteListCount() ?: 0
                val latestBlocked = callRepo?.getLatestBlockedCall()

                for (widgetId in allWidgetIds) {
                    val compactViews = buildWidgetViews(
                        context = context,
                        layoutRes = R.layout.widget_white_call_compact,
                        widgetType = TYPE_COMPACT,
                        isProtectionActive = isProtectionActive,
                        isScheduleEnabled = isScheduleEnabled,
                        blockedToday = blockedToday,
                        blockedWeek = blockedWeek,
                        blockedMonth = blockedMonth,
                        whiteListCount = whiteListCount,
                        latestBlocked = latestBlocked,
                        isAlertFlashing = isAlertFlashing,
                        alertCallerDisplay = alertCallerDisplay
                    )

                    val expandedViews = buildWidgetViews(
                        context = context,
                        layoutRes = R.layout.widget_white_call_expanded,
                        widgetType = TYPE_EXPANDED,
                        isProtectionActive = isProtectionActive,
                        isScheduleEnabled = isScheduleEnabled,
                        blockedToday = blockedToday,
                        blockedWeek = blockedWeek,
                        blockedMonth = blockedMonth,
                        whiteListCount = whiteListCount,
                        latestBlocked = latestBlocked,
                        isAlertFlashing = isAlertFlashing,
                        alertCallerDisplay = alertCallerDisplay
                    )

                    val tallViews = buildWidgetViews(
                        context = context,
                        layoutRes = R.layout.widget_white_call_tall,
                        widgetType = TYPE_TALL,
                        isProtectionActive = isProtectionActive,
                        isScheduleEnabled = isScheduleEnabled,
                        blockedToday = blockedToday,
                        blockedWeek = blockedWeek,
                        blockedMonth = blockedMonth,
                        whiteListCount = whiteListCount,
                        latestBlocked = latestBlocked,
                        isAlertFlashing = isAlertFlashing,
                        alertCallerDisplay = alertCallerDisplay
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val viewsMap = mapOf(
                            SizeF(80f, 40f) to compactViews,    // 2x1
                            SizeF(150f, 40f) to expandedViews,  // 3x1, 4x1, 5x1
                            SizeF(80f, 70f) to tallViews,       // 2x2
                            SizeF(150f, 70f) to tallViews       // 3x2, 4x2, 5x2
                        )
                        appWidgetManager.updateAppWidget(widgetId, RemoteViews(viewsMap))
                    } else {
                        val options = appWidgetManager.getAppWidgetOptions(widgetId)
                        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
                        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
                        val chosenViews = when {
                            minHeight >= 70 -> tallViews
                            minWidth >= 150 -> expandedViews
                            else -> compactViews
                        }
                        appWidgetManager.updateAppWidget(widgetId, chosenViews)
                    }
                }
            }
        }

        private const val TYPE_COMPACT = 1
        private const val TYPE_EXPANDED = 2
        private const val TYPE_TALL = 3

        private fun buildWidgetViews(
            context: Context,
            layoutRes: Int,
            widgetType: Int,
            isProtectionActive: Boolean,
            isScheduleEnabled: Boolean,
            blockedToday: Int,
            blockedWeek: Int,
            blockedMonth: Int,
            whiteListCount: Int,
            latestBlocked: com.whitecall.app.domain.model.BlockedCallLog?,
            isAlertFlashing: Boolean,
            alertCallerDisplay: String?
        ): RemoteViews {
            val views = RemoteViews(context.packageName, layoutRes)

            // Status colors and icons
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

            // Left status icon
            val iconSize = if (widgetType == TYPE_COMPACT) 26 else 32
            val statusBitmap = drawableToBitmap(context, iconRes, statusColor, iconSize, iconSize)
            if (statusBitmap != null) {
                views.setImageViewBitmap(R.id.widget_status_icon, statusBitmap)
            }

            // Right Power Button
            val powerSize = if (widgetType == TYPE_COMPACT) 36 else 44
            val powerBitmap = drawableToBitmap(context, R.drawable.ic_power, powerColor, powerSize, powerSize)
            if (powerBitmap != null) {
                views.setImageViewBitmap(R.id.widget_toggle_button, powerBitmap)
            }

            val greenColor = ContextCompat.getColor(context, R.color.widget_status_active)

            // Fill content based on type
            when (widgetType) {
                TYPE_COMPACT -> {
                    // Compact 2x1: Line 1 = WC, Line 2 = Alert or "Блок: N"
                    if (alertCallerDisplay != null) {
                        val phoneBitmap = drawableToBitmap(context, R.drawable.ic_phone_incoming, greenColor, 20, 20)
                        if (phoneBitmap != null) {
                            views.setImageViewBitmap(R.id.widget_incoming_icon, phoneBitmap)
                        }
                        views.setViewVisibility(R.id.widget_incoming_icon, if (isAlertFlashing) View.VISIBLE else View.INVISIBLE)
                        views.setTextViewText(R.id.widget_status_text, alertCallerDisplay)
                        views.setTextColor(R.id.widget_status_text, greenColor)
                    } else {
                        views.setViewVisibility(R.id.widget_incoming_icon, View.GONE)
                        views.setTextViewText(R.id.widget_status_text, context.getString(R.string.widget_blocked_short, blockedToday))
                        views.setTextColor(R.id.widget_status_text, ContextCompat.getColor(context, R.color.widget_text_secondary))
                    }
                }
                TYPE_EXPANDED -> {
                    // Expanded 3x1..5x1: 1 horizontal row: Icon + WhiteCall + Status/Alert + Badge (Блок: N) + Power
                    if (alertCallerDisplay != null) {
                        val phoneBitmap = drawableToBitmap(context, R.drawable.ic_phone_incoming, greenColor, 20, 20)
                        if (phoneBitmap != null) {
                            views.setImageViewBitmap(R.id.widget_incoming_icon, phoneBitmap)
                        }
                        views.setViewVisibility(R.id.widget_incoming_icon, if (isAlertFlashing) View.VISIBLE else View.INVISIBLE)
                        views.setTextViewText(R.id.widget_status_text, alertCallerDisplay)
                        views.setTextColor(R.id.widget_status_text, greenColor)
                    } else {
                        views.setViewVisibility(R.id.widget_incoming_icon, View.GONE)
                        views.setTextViewText(R.id.widget_status_text, statusText)
                        views.setTextColor(R.id.widget_status_text, statusColor)
                    }
                    views.setTextViewText(R.id.widget_blocked_badge, context.getString(R.string.widget_blocked_short, blockedToday))
                    views.setTextColor(R.id.widget_blocked_badge, ContextCompat.getColor(context, R.color.widget_text_primary))
                }
                TYPE_TALL -> {
                    // Tall 2x2..5x2: Top = WhiteCall + Status, Middle = Stats (День/Нед/Мес + Список), Bottom = Last Call or Alert
                    views.setTextViewText(R.id.widget_status_text, statusText)
                    views.setTextColor(R.id.widget_status_text, statusColor)

                    views.setTextViewText(
                        R.id.widget_tall_stats,
                        "Блок: День: $blockedToday • Нед: $blockedWeek • Мес: $blockedMonth"
                    )
                    views.setTextViewText(R.id.widget_tall_whitelist_count, "👥 $whiteListCount")

                    if (alertCallerDisplay != null) {
                        val phoneBitmap = drawableToBitmap(context, R.drawable.ic_phone_incoming, greenColor, 24, 24)
                        if (phoneBitmap != null) {
                            views.setImageViewBitmap(R.id.widget_incoming_icon, phoneBitmap)
                        }
                        views.setViewVisibility(R.id.widget_incoming_icon, if (isAlertFlashing) View.VISIBLE else View.INVISIBLE)
                        views.setTextViewText(R.id.widget_last_call_text, "Входящий: $alertCallerDisplay")
                        views.setTextColor(R.id.widget_last_call_text, greenColor)
                    } else if (latestBlocked != null) {
                        val callerDisplay = latestBlocked.callerName ?: latestBlocked.phoneNumber
                        val timeDisplay = PhoneUtils.formatTimeOnly(latestBlocked.timestamp)
                        views.setViewVisibility(R.id.widget_incoming_icon, View.GONE)
                        views.setTextViewText(R.id.widget_last_call_text, "Последний: $callerDisplay • $timeDisplay")
                        views.setTextColor(R.id.widget_last_call_text, ContextCompat.getColor(context, R.color.widget_text_secondary))
                    } else {
                        views.setViewVisibility(R.id.widget_incoming_icon, View.GONE)
                        views.setTextViewText(R.id.widget_last_call_text, "Нет заблокированных звонков")
                        views.setTextColor(R.id.widget_last_call_text, ContextCompat.getColor(context, R.color.widget_text_secondary))
                    }
                }
            }

            // Tap root -> open app
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

            // Tap power button -> toggle protection
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

            return views
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
