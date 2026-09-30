package com.whitecall.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object UpdateChecker {

    const val GITHUB_REPO_URL = "https://github.com/EvgeniyKrasnyanskiy/WhiteCall"

    fun openGitHub(context: Context) {
        openUrl(context, GITHUB_REPO_URL)
    }

    fun openUrl(context: Context, urlString: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

