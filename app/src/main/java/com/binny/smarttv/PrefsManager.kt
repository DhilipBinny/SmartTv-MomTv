package com.binny.smarttv

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray

object PrefsManager {

    private const val PREFS_NAME = "momtv_prefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_RECENTS = "recents"
    private const val KEY_HIDDEN = "hidden"
    private const val KEY_DND_PREV = "dnd_previous_filter"
    private const val KEY_WEATHER_TEMP = "weather_temp"
    private const val KEY_WEATHER_CODE = "weather_code"
    private const val KEY_WEATHER_TIME = "weather_time"
    private const val MAX_RECENTS = 6

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getStringList(context: Context, key: String): MutableList<String> {
        return try {
            val json = prefs(context).getString(key, "[]") ?: "[]"
            val arr = JSONArray(json)
            MutableList(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            Log.e("MomTV", "Failed to parse prefs key=$key, resetting", e)
            mutableListOf()
        }
    }

    private fun saveStringList(context: Context, key: String, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs(context).edit().putString(key, arr.toString()).apply()
    }

    private fun toggleInSet(context: Context, key: String, value: String): Boolean {
        val items = getStringList(context, key)
        val added = if (items.contains(value)) {
            items.remove(value); false
        } else {
            items.add(value); true
        }
        saveStringList(context, key, items)
        return added
    }

    fun getFavorites(context: Context): Set<String> = getStringList(context, KEY_FAVORITES).toSet()

    fun toggleFavorite(context: Context, packageName: String): Boolean =
        toggleInSet(context, KEY_FAVORITES, packageName)

    fun getRecents(context: Context): List<String> = getStringList(context, KEY_RECENTS)

    fun recordRecent(context: Context, packageName: String) {
        val recents = getStringList(context, KEY_RECENTS)
        recents.remove(packageName)
        recents.add(0, packageName)
        if (recents.size > MAX_RECENTS) recents.removeAt(recents.lastIndex)
        saveStringList(context, KEY_RECENTS, recents)
    }

    fun getHidden(context: Context): Set<String> = getStringList(context, KEY_HIDDEN).toSet()

    fun toggleHidden(context: Context, packageName: String): Boolean =
        toggleInSet(context, KEY_HIDDEN, packageName)

    fun pruneOrphans(context: Context, installedPkgs: Set<String>) {
        var changed = false

        val favs = getStringList(context, KEY_FAVORITES)
        val prunedFavs = favs.filter { it in installedPkgs }
        if (prunedFavs.size != favs.size) {
            saveStringList(context, KEY_FAVORITES, prunedFavs); changed = true
        }

        val recs = getStringList(context, KEY_RECENTS)
        val prunedRecs = recs.filter { it in installedPkgs }
        if (prunedRecs.size != recs.size) {
            saveStringList(context, KEY_RECENTS, prunedRecs); changed = true
        }

        if (changed) Log.d("MomTV", "Pruned orphaned prefs entries")
    }

    fun saveDndState(context: Context, filter: Int) {
        prefs(context).edit().putInt(KEY_DND_PREV, filter).apply()
    }

    fun getSavedDndState(context: Context): Int {
        return prefs(context).getInt(KEY_DND_PREV, NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    fun cacheWeather(context: Context, data: WeatherData) {
        prefs(context).edit()
            .putFloat(KEY_WEATHER_TEMP, data.temperature.toFloat())
            .putInt(KEY_WEATHER_CODE, data.weatherCode)
            .putLong(KEY_WEATHER_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getCachedWeather(context: Context): WeatherData? {
        val p = prefs(context)
        if (!p.contains(KEY_WEATHER_TEMP)) return null
        return WeatherData(
            temperature = p.getFloat(KEY_WEATHER_TEMP, 0f).toDouble(),
            weatherCode = p.getInt(KEY_WEATHER_CODE, 0)
        )
    }
}
