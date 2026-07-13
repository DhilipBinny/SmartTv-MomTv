package com.binny.smarttv

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    val showScreensaver = mutableStateOf(false)
    val showQuickSettings = mutableStateOf(false)
    val resumeCounter = mutableStateOf(0)
    private var idleRunnable: Runnable? = null
    private var swallowNextUp = false
    private val idleTimeoutMs = 3L * 60 * 1000

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
        resetIdleTimer()

        setContent {
            MomTVApp(
                showScreensaver = showScreensaver.value,
                showQuickSettings = showQuickSettings.value,
                resumeTick = resumeCounter.value,
                onDismissScreensaver = {
                    showScreensaver.value = false
                    resetIdleTimer()
                },
                onDismissQuickSettings = {
                    showQuickSettings.value = false
                    resetIdleTimer()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCounter.value++
        resetIdleTimer()
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

        // Menu key toggles quick settings
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_DOWN) {
            showQuickSettings.value = !showQuickSettings.value
            resetIdleTimer()
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

    private var previousInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL

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

    private fun resetIdleTimer() {
        idleRunnable?.let { window.decorView.removeCallbacks(it) }
        idleRunnable = Runnable { showScreensaver.value = true }
        window.decorView.postDelayed(idleRunnable!!, idleTimeoutMs)
    }

    override fun onPause() {
        super.onPause()
        idleRunnable?.let { window.decorView.removeCallbacks(it) }
    }

    override fun onDestroy() {
        idleRunnable?.let { window.decorView.removeCallbacks(it) }
        restoreDndMode()
        super.onDestroy()
    }
}
