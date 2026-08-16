package com.whitecall.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.whitecall.app.R

sealed class NavDestination(
    val route: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
) {
    object WhiteList : NavDestination(
        route = "whitelist",
        titleRes = R.string.tab_whitelist,
        iconRes = R.drawable.ic_list
    )

    object BlockedLog : NavDestination(
        route = "blocked_log",
        titleRes = R.string.tab_blocked_log,
        iconRes = R.drawable.ic_history
    )

    object Settings : NavDestination(
        route = "settings",
        titleRes = R.string.tab_settings,
        iconRes = R.drawable.ic_settings
    )

    companion object {
        val items = listOf(WhiteList, BlockedLog, Settings)
    }
}
