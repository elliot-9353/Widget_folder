package com.example.folderwidget.ui

import android.content.Context
import android.content.Intent

data class AppInfo(val label: String, val pkg: String)

/** All apps that appear in the launcher, sorted by name (this app excluded). */
fun loadLaunchableApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .distinctBy { it.pkg }
        .filter { it.pkg != ctx.packageName }
        .sortedBy { it.label.lowercase() }
}
