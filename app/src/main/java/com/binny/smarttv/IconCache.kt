package com.binny.smarttv

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

object IconCache {

    private val cache = mutableMapOf<String, ImageBitmap>()
    private var lastPackageHash = 0

    fun getIcon(pkg: String): ImageBitmap? = cache[pkg]

    fun loadIcons(context: Context, packages: Set<String>): Boolean {
        val hash = packages.hashCode()
        if (hash == lastPackageHash && cache.isNotEmpty()) return false

        val pm = context.packageManager
        val toLoad = packages - cache.keys
        for (pkg in toLoad) {
            try {
                val icon = pm.getApplicationIcon(pkg)
                cache[pkg] = icon.toBitmap(96, 96).asImageBitmap()
            } catch (_: PackageManager.NameNotFoundException) {}
            catch (_: Exception) {}
        }

        val removed = cache.keys - packages
        removed.forEach { cache.remove(it) }

        lastPackageHash = hash
        return toLoad.isNotEmpty()
    }

    fun clear() {
        cache.clear()
        lastPackageHash = 0
    }
}
