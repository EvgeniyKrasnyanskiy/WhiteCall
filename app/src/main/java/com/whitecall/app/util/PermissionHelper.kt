package com.whitecall.app.util

import android.app.role.RoleManager
import android.content.Context
import android.os.Build

object PermissionHelper {
    fun isCallScreeningRoleHeld(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            true
        }
    }
}
