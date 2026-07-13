package com.binny.smarttv

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

object PrefsManager {

    private const val PREFS_NAME = "momtv_prefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_RECENTS = "recents"
    private const val KEY_HIDDEN = "hidden"
    private const val MAX_RECENTS = 6

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getStringSet(context: Context, key: String): MutableSet<String> {
        val json = prefs(context).getString(key, "[]") ?: "[]"
        val arr = JSONArray(json)
        val set = mutableSetOf<String>()
        for (i in 0 until arr.length()) set.add(arr.getString(i))
        return set
    }

    private fun getStringList(context: Context, key: String): MutableList<String> {
        val json = prefs(context).getString(key, "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        return list
    }

    private fun saveStringList(context: Context, key: String, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs(context).edit().putString(key, arr.toString()).apply()
    }

    fun getFavorites(context: Context): Set<String> = getStringSet(context, KEY_FAVORITES)

    fun toggleFavorite(context: Context, packageName: String): Boolean {
        val favs = getStringSet(context, KEY_FAVORITES)
        val added = if (favs.contains(packageName)) {
            favs.remove(packageName); false
        } else {
            favs.add(packageName); true
        }
        saveStringList(context, KEY_FAVORITES, favs.toList())
        return added
    }

    fun getRecents(context: Context): List<String> = getStringList(context, KEY_RECENTS)

    fun recordRecent(context: Context, packageName: String) {
        val recents = getStringList(context, KEY_RECENTS)
        recents.remove(packageName)
        recents.add(0, packageName)
        if (recents.size > MAX_RECENTS) recents.removeAt(recents.lastIndex)
        saveStringList(context, KEY_RECENTS, recents)
    }

    fun getHidden(context: Context): Set<String> = getStringSet(context, KEY_HIDDEN)

    fun toggleHidden(context: Context, packageName: String): Boolean {
        val hidden = getStringSet(context, KEY_HIDDEN)
        val nowHidden = if (hidden.contains(packageName)) {
            hidden.remove(packageName); false
        } else {
            hidden.add(packageName); true
        }
        saveStringList(context, KEY_HIDDEN, hidden.toList())
        return nowHidden
    }
}
