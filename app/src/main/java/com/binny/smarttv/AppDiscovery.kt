package com.binny.smarttv

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap

data class TvApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap?,
    val category: AppCategory
)

enum class AppCategory(val title: String) {
    WATCH("Watch"),
    MUSIC("Music"),
    APPS("Apps"),
    SETTINGS("Settings")
}

object AppDiscovery {

    private val watchApps = setOf(
        "com.google.android.youtube", "org.niceram.niceplayer.free", "com.teamsmart.videomanager.tv",
        "com.netflix.mediaclient", "com.hotstar.android", "in.startv.hotstar",
        "com.amazon.avod.thirdpartyclient", "com.amazon.amazonvideo.livingroom",
        "tv.accedo.airtel.wynk", "com.jio.jioplay.tv", "com.sonyliv", "com.zee5.hikeapp",
        "com.graymatrix.did", "com.mxtech.videoplayer.ad", "org.videolan.vlc", "com.kodi",
        "com.disney.disneyplus", "com.jio.media.ondemand",
    )

    private val musicApps = setOf(
        "com.google.android.apps.youtube.music", "com.spotify.music",
        "com.gaana", "com.jio.media.jiobeats", "com.apple.android.music",
    )

    private val settingsEntries = listOf(
        TvApp("Wi-Fi", "settings.wifi", null, AppCategory.SETTINGS),
        TvApp("Bluetooth", "settings.bluetooth", null, AppCategory.SETTINGS),
        TvApp("Display", "settings.display", null, AppCategory.SETTINGS),
        TvApp("Sound", "settings.sound", null, AppCategory.SETTINGS),
    )

    fun discoverApps(context: Context): List<TvApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolvedApps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val hidden = PrefsManager.getHidden(context)

        val seen = mutableSetOf<String>()
        val appPkgs = mutableListOf<Triple<String, String, ApplicationInfo?>>()

        for (info: ResolveInfo in resolvedApps) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || !seen.add(pkg)) continue
            if (pkg in hidden) continue
            appPkgs.add(Triple(pkg, info.loadLabel(pm).toString(), info.activityInfo.applicationInfo))
        }

        IconCache.loadIcons(context, seen)

        val apps = appPkgs.map { (pkg, label, appInfo) ->
            TvApp(label, pkg, IconCache.getIcon(pkg), categorize(pkg, appInfo))
        }.sortedBy { it.label.lowercase() }.toMutableList()

        apps.addAll(settingsEntries)
        PrefsManager.pruneOrphans(context, seen)
        return apps
    }

    fun discoverAllIncludingHidden(context: Context): List<Pair<TvApp, Boolean>> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolvedApps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val hidden = PrefsManager.getHidden(context)
        val seen = mutableSetOf<String>()

        return resolvedApps.mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || !seen.add(pkg)) return@mapNotNull null
            val label = info.loadLabel(pm).toString()
            val icon = IconCache.getIcon(pkg)
            val isHidden = pkg in hidden
            Pair(TvApp(label, pkg, icon, categorize(pkg, info.activityInfo.applicationInfo)), isHidden)
        }.sortedBy { it.first.label.lowercase() }
    }

    private fun categorize(pkg: String, appInfo: ApplicationInfo?): AppCategory {
        if (pkg in watchApps) return AppCategory.WATCH
        if (pkg in musicApps) return AppCategory.MUSIC
        val cat = appInfo?.category ?: return AppCategory.APPS
        return when (cat) {
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.WATCH
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.MUSIC
            else -> AppCategory.APPS
        }
    }

    fun launchApp(context: Context, app: TvApp) {
        try {
            if (app.category == AppCategory.SETTINGS) {
                val intent = when (app.packageName) {
                    "settings.wifi" -> Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    "settings.bluetooth" -> Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                    "settings.display" -> Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
                    "settings.sound" -> Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
                    else -> Intent(android.provider.Settings.ACTION_SETTINGS)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                PrefsManager.recordRecent(context, app.packageName)
            }
        } catch (e: Exception) {
            Log.e("MomTV", "Failed to launch ${app.label}", e)
        }
    }

    fun openCastSettings(context: Context) {
        try {
            context.startActivity(Intent("android.settings.CAST_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            try { context.startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
        }
    }
}
