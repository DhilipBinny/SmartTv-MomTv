package com.binny.smarttv

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    val showScreensaver = mutableStateOf(false)
    val showQuickSettings = mutableStateOf(false)
    val showVolumeOverlay = mutableStateOf(false)
    val volumeLevel = mutableFloatStateOf(0.5f)
    val resumeCounter = mutableIntStateOf(0)
    val isDefaultLauncher = mutableStateOf(true)

    private var idleRunnable: Runnable? = null
    private var volumeHideRunnable: Runnable? = null
    private var swallowNextUp = false
    private val idleTimeoutMs = 3L * 60 * 1000
    private var previousInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        enableDndMode()
        requestRuntimePermissions()
        initLocationWeather()
        checkDefaultLauncher()
        resetIdleTimer()

        setContent {
            MomTVApp(
                showScreensaver = showScreensaver.value,
                showQuickSettings = showQuickSettings.value,
                showVolumeOverlay = showVolumeOverlay.value,
                volumeLevel = volumeLevel.floatValue,
                resumeTick = resumeCounter.intValue,
                isDefaultLauncher = isDefaultLauncher.value,
                onDismissScreensaver = {
                    showScreensaver.value = false
                    resetIdleTimer()
                },
                onDismissQuickSettings = {
                    showQuickSettings.value = false
                    resetIdleTimer()
                },
                onSetDefaultLauncher = { promptSetDefaultLauncher() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCounter.intValue++
        checkDefaultLauncher()
        resetIdleTimer()
    }

    override fun onBackPressed() {
        // Launcher should never exit on back press
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.dispatchKeyEvent(event)

        // Screensaver: any key dismisses
        if (showScreensaver.value && event.action == KeyEvent.ACTION_DOWN) {
            showScreensaver.value = false
            resetIdleTimer()
            swallowNextUp = true
            return true
        }
        if (swallowNextUp && event.action == KeyEvent.ACTION_UP) {
            swallowNextUp = false
            return true
        }

        // Volume keys: custom overlay
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    handleVolumeKey(event.keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                    resetIdleTimer()
                    return true
                }
                KeyEvent.KEYCODE_MENU -> {
                    showQuickSettings.value = !showQuickSettings.value
                    resetIdleTimer()
                    return true
                }
            }
        }
        // Suppress volume key up events too
        if (event.action == KeyEvent.ACTION_UP &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true
        }

        resetIdleTimer()
        return super.dispatchKeyEvent(event)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (showScreensaver.value) showScreensaver.value = false
        resetIdleTimer()
    }

    private fun handleVolumeKey(up: Boolean) {
        val am = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val newVol = if (up) (current + 1).coerceAtMost(max) else (current - 1).coerceAtLeast(0)
        am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
        volumeLevel.floatValue = newVol.toFloat() / max.toFloat()
        showVolumeOverlay.value = true

        volumeHideRunnable?.let { window.decorView.removeCallbacks(it) }
        volumeHideRunnable = Runnable { showVolumeOverlay.value = false }
        window.decorView.postDelayed(volumeHideRunnable!!, 2000)
    }

    private fun enableDndMode() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                previousInterruptionFilter = nm.currentInterruptionFilter
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        } catch (_: Exception) {}
    }

    private fun restoreDndMode() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(previousInterruptionFilter)
            }
        } catch (_: Exception) {}
    }

    private fun initLocationWeather() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            @Suppress("DEPRECATION")
            val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc != null) {
                WeatherService.setLocation(loc.latitude, loc.longitude)
            }
        } catch (_: SecurityException) {
            // Location permission not granted — use default city
        } catch (_: Exception) {}
    }

    private fun checkDefaultLauncher() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        isDefaultLauncher.value = resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun promptSetDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                startActivity(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        val storagePermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            needed.add(storagePermission)
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }

    private fun resetIdleTimer() {
        idleRunnable?.let { window.decorView.removeCallbacks(it) }
        idleRunnable = Runnable { showScreensaver.value = true }
        window.decorView.postDelayed(idleRunnable!!, idleTimeoutMs)
    }

    override fun onPause() {
        super.onPause()
        idleRunnable?.let { window.decorView.removeCallbacks(it) }
        volumeHideRunnable?.let { window.decorView.removeCallbacks(it) }
    }

    override fun onDestroy() {
        idleRunnable?.let { window.decorView.removeCallbacks(it) }
        volumeHideRunnable?.let { window.decorView.removeCallbacks(it) }
        restoreDndMode()
        super.onDestroy()
    }
}
