package com.binny.smarttv

import android.content.ContentUris
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private val Bg = Color(0xFF000000)
private val Surface = Color(0xFF1C1C1E)
private val SurfaceFocused = Color(0xFF2C2C2E)
private val TextW = Color(0xFFFFFFFF)
private val TextSec = Color(0xFF8E8E93)
private val TextTer = Color(0xFF48484A)
private val FocusGlow = Color(0xFFFFFFFF)
private val AccentBlue = Color(0xFF0A84FF)

@Composable
fun MomTVApp(
    showScreensaver: Boolean,
    showQuickSettings: Boolean,
    showVolumeOverlay: Boolean,
    volumeLevel: Float,
    resumeTick: Int = 0,
    isDefaultLauncher: Boolean = true,
    locationAvailable: Boolean = true,
    onDismissScreensaver: () -> Unit,
    onDismissQuickSettings: () -> Unit,
    onSetDefaultLauncher: () -> Unit = {}
) {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf(emptyList<TvApp>()) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var recents by remember { mutableStateOf(emptyList<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var contextApp by remember { mutableStateOf<TvApp?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    // Lifecycle-aware reload — fixes recents not appearing on resume
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var lifecycleTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) lifecycleTick++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshTick, resumeTick, lifecycleTick) {
        allApps = try {
            withContext(Dispatchers.IO) { AppDiscovery.discoverApps(context) }
        } catch (e: Exception) {
            Log.e("MomTV", "Discovery failed", e)
            emptyList()
        }
        favorites = withContext(Dispatchers.IO) { PrefsManager.getFavorites(context) }
        recents = withContext(Dispatchers.IO) { PrefsManager.getRecents(context) }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 28.dp, bottom = 12.dp)) {
            TopBar(
                isDefaultLauncher = isDefaultLauncher,
                onSetDefault = onSetDefaultLauncher,
                onOpenSettings = { showSettings = true }
            )
            Spacer(modifier = Modifier.height(28.dp))

            if (isLoading) {
                LoadingSkeleton()
            } else if (allApps.isEmpty()) {
                EmptyState()
            } else {
                AppGrid(
                    allApps = allApps, favorites = favorites, recents = recents,
                    onLaunch = { app -> AppDiscovery.launchApp(context, app); refreshTick++ },
                    onLongPress = { app -> contextApp = app }
                )
            }
        }

        // Overlays in priority order
        contextApp?.let { app ->
            ContextMenu(
                app = app, isFavorite = app.packageName in favorites,
                onDismiss = { contextApp = null },
                onToggleFavorite = { PrefsManager.toggleFavorite(context, app.packageName); refreshTick++; contextApp = null },
                onHide = { PrefsManager.toggleHidden(context, app.packageName); refreshTick++; contextApp = null }
            )
        }

        if (showSettings) {
            SettingsPanel(
                onDismiss = { showSettings = false; refreshTick++ }
            )
        }

        AnimatedVisibility(visible = showQuickSettings, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            QuickSettingsPanel(onDismiss = onDismissQuickSettings)
        }

        AnimatedVisibility(visible = showVolumeOverlay, enter = fadeIn(tween(100)), exit = fadeOut(tween(300))) {
            VolumeOverlay(level = volumeLevel)
        }

        AnimatedVisibility(visible = showScreensaver, enter = fadeIn(tween(1200)), exit = fadeOut(tween(400))) {
            PhotoScreensaver(onDismiss = onDismissScreensaver)
        }
    }
}

// ─── Loading & Empty States ────────────────────────────────

@Composable
private fun LoadingSkeleton() {
    val shimmer = rememberInfiniteTransition(label = "sh")
    val alpha by shimmer.animateFloat(0.15f, 0.35f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")

    Column(modifier = Modifier.padding(horizontal = 48.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(TextTer.copy(alpha = alpha)))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(5) {
                        Box(Modifier.width(130.dp).height(105.dp).clip(RoundedCornerShape(16.dp)).background(Surface.copy(alpha = alpha)))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No apps found", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextSec))
            Spacer(Modifier.height(8.dp))
            Text("Install some apps to get started", style = TextStyle(fontSize = 13.sp, color = TextTer))
        }
    }
}

// ─── Top Bar ───────────────────────────────────────────────

@Composable
private fun TopBar(isDefaultLauncher: Boolean, onSetDefault: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var greeting by remember { mutableStateOf("") }
    var weather by remember { mutableStateOf<WeatherData?>(null) }

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        while (true) {
            val now = Date()
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            time = timeFmt.format(now); date = dateFmt.format(now)
            greeting = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
            delay(15_000)
        }
    }

    LaunchedEffect(Unit) {
        weather = withContext(Dispatchers.IO) { PrefsManager.getCachedWeather(context) }
        val fresh = withContext(Dispatchers.IO) { WeatherService.fetchWeather() }
        if (fresh != null) { weather = fresh; PrefsManager.cacheWeather(context, fresh) }
        while (true) {
            delay(30 * 60 * 1000L)
            val w = withContext(Dispatchers.IO) { WeatherService.fetchWeather() }
            if (w != null) { weather = w; PrefsManager.cacheWeather(context, w) }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { onOpenSettings() }) }) {
                Text(greeting, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = TextSec, letterSpacing = 0.5.sp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(date, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = TextTer))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                weather?.let { w ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(w.icon, fontSize = 18.sp)
                        Text(w.tempDisplay, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSec))
                    }
                }
                Text(time, style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Thin, color = TextW.copy(alpha = 0.9f), letterSpacing = 2.sp))
            }
        }

        if (!isDefaultLauncher) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap here to set MomTV as your default launcher",
                style = TextStyle(fontSize = 11.sp, color = AccentBlue),
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentBlue.copy(alpha = 0.1f)).clickable { onSetDefault() }.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

// ─── Settings Panel (Unhide apps) ──────────────────────────

@Composable
private fun SettingsPanel(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var allWithHidden by remember { mutableStateOf(emptyList<Pair<TvApp, Boolean>>()) }

    LaunchedEffect(Unit) {
        allWithHidden = withContext(Dispatchers.IO) { AppDiscovery.discoverAllIncludingHidden(context) }
    }

    val hiddenApps = allWithHidden.filter { it.second }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable(onClick = onDismiss)
            .onKeyEvent { if (it.type == KeyEventType.KeyDown && it.key == Key.Back) { onDismiss(); true } else false },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(400.dp).heightIn(max = 500.dp)
                .clip(RoundedCornerShape(20.dp)).background(Surface).padding(24.dp)
                .clickable(enabled = false, onClick = {}),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Settings", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextW))

            Box(Modifier.fillMaxWidth().height(0.5.dp).background(TextTer.copy(alpha = 0.2f)))

            if (hiddenApps.isEmpty()) {
                Text("No hidden apps", style = TextStyle(fontSize = 13.sp, color = TextTer), modifier = Modifier.padding(vertical = 16.dp))
            } else {
                Text("Hidden Apps", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSec, letterSpacing = 1.sp))

                val firstFr = remember { FocusRequester() }
                LaunchedEffect(Unit) { try { firstFr.requestFocus() } catch (_: Exception) {} }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f, false)) {
                    items(hiddenApps, key = { it.first.packageName }) { (app, _) ->
                        val fr = remember { FocusRequester() }
                        val mod = if (app == hiddenApps.first().first) Modifier.focusRequester(firstFr) else Modifier.focusRequester(fr)
                        UnhideRow(app = app, modifier = mod) {
                            PrefsManager.toggleHidden(context, app.packageName)
                            allWithHidden = allWithHidden.map {
                                if (it.first.packageName == app.packageName) Pair(it.first, false) else it
                            }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(0.5.dp).background(TextTer.copy(alpha = 0.2f)))

            MenuButton(text = "Close", onClick = onDismiss)
        }
    }
}

@Composable
private fun UnhideRow(app: TvApp, modifier: Modifier = Modifier, onUnhide: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (focused) AccentBlue.copy(alpha = 0.2f) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }.focusable()
            .clickable(onClick = onUnhide)
            .onKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.DirectionCenter)) { onUnhide(); true } else false }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (app.icon != null) {
                Image(painter = remember(app.icon) { BitmapPainter(app.icon) }, contentDescription = null, modifier = Modifier.size(28.dp))
            }
            Text(app.label, style = TextStyle(fontSize = 13.sp, color = if (focused) TextW else TextSec))
        }
        Text("Unhide", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue))
    }
}

// ─── Volume Overlay ────────────────────────────────────────

@Composable
private fun VolumeOverlay(level: Float) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier = Modifier.padding(top = 80.dp).width(200.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceFocused.copy(alpha = 0.92f)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("♪", fontSize = 16.sp, color = TextSec)
            SliderBar(value = level, modifier = Modifier.weight(1f))
            Text("${(level * 100).toInt()}%", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextW, fontFamily = FontFamily.Monospace))
        }
    }
}

// ─── App Grid ──────────────────────────────────────────────

@Composable
private fun AppGrid(
    allApps: List<TvApp>, favorites: Set<String>, recents: List<String>,
    onLaunch: (TvApp) -> Unit, onLongPress: (TvApp) -> Unit
) {
    val appMap = remember(allApps) { allApps.associateBy { it.packageName } }
    val grouped = remember(allApps) { allApps.groupBy { it.category } }
    val favApps = remember(favorites, appMap) { favorites.mapNotNull { appMap[it] } }
    val recentApps = remember(recents, appMap, favorites) { recents.filter { it !in favorites }.mapNotNull { appMap[it] } }

    data class RowData(val key: String, val title: String, val apps: List<TvApp>, val isFirst: Boolean = false)
    val rows = remember(favApps, recentApps, grouped) {
        val r = mutableListOf<RowData>()
        if (favApps.isNotEmpty()) r.add(RowData("fav", "Favorites", favApps, isFirst = true))
        if (recentApps.isNotEmpty()) r.add(RowData("rec", "Recent", recentApps, isFirst = r.isEmpty()))
        for (cat in listOf(AppCategory.WATCH, AppCategory.MUSIC, AppCategory.APPS, AppCategory.SETTINGS)) {
            val apps = grouped[cat] ?: continue; if (apps.isEmpty()) continue
            r.add(RowData(cat.name, cat.title, apps, isFirst = r.isEmpty()))
        }
        r
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items(rows, key = { it.key }) { row ->
            AppRow(title = row.title, apps = row.apps, isFirst = row.isFirst, onLaunch = onLaunch, onLongPress = onLongPress)
        }
    }
}

@Composable
private fun AppRow(title: String, apps: List<TvApp>, isFirst: Boolean, onLaunch: (TvApp) -> Unit, onLongPress: (TvApp) -> Unit) {
    val listState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    if (isFirst) { LaunchedEffect(Unit) { try { firstFocus.requestFocus() } catch (_: Exception) {} } }

    Column {
        Text(title.uppercase(), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextTer, letterSpacing = 2.sp), modifier = Modifier.padding(start = 48.dp, bottom = 10.dp))
        LazyRow(state = listState, contentPadding = PaddingValues(horizontal = 48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps, key = { "${title}_${it.packageName}" }) { app ->
                val fr = remember { FocusRequester() }
                AppCard(app = app, modifier = if (isFirst && app == apps.first()) Modifier.focusRequester(firstFocus) else Modifier.focusRequester(fr), onLaunch = { onLaunch(app) }, onLongPress = { onLongPress(app) })
            }
        }
    }
}

@Composable
private fun AppCard(app: TvApp, modifier: Modifier = Modifier, onLaunch: () -> Unit, onLongPress: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(180, easing = FastOutSlowInEasing), label = "s")
    val borderAlpha by animateFloatAsState(if (focused) 0.5f else 0f, tween(150), label = "b")
    val isSettings = app.category == AppCategory.SETTINGS
    val w = if (isSettings) 110.dp else 130.dp; val h = if (isSettings) 85.dp else 105.dp
    var centerDownTime by remember { mutableStateOf(0L) }

    Box(
        modifier = modifier.width(w).height(h).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp)).background(if (focused) SurfaceFocused else Surface)
            .then(if (focused) Modifier.border(1.5.dp, FocusGlow.copy(alpha = borderAlpha), RoundedCornerShape(16.dp)) else Modifier)
            .onFocusChanged { focused = it.isFocused }.focusable()
            .pointerInput(Unit) { detectTapGestures(onTap = { onLaunch() }, onLongPress = { onLongPress() }) }
            .semantics { contentDescription = app.label }
            .onPreviewKeyEvent { event ->
                val isConfirm = event.key == Key.Enter || event.key == Key.DirectionCenter
                val isRepeat = event.nativeKeyEvent.repeatCount > 0
                when {
                    isConfirm && !isRepeat -> {
                        if (event.type == KeyEventType.KeyDown) {
                            if (centerDownTime == 0L) centerDownTime = System.currentTimeMillis()
                            if (System.currentTimeMillis() - centerDownTime > 600) { onLongPress(); centerDownTime = 0L; true } else false
                        } else if (event.type == KeyEventType.KeyUp) {
                            val held = System.currentTimeMillis() - centerDownTime; centerDownTime = 0L
                            if (held in 1..600) { onLaunch(); true } else true
                        } else false
                    }
                    isConfirm && isRepeat -> false
                    event.type == KeyEventType.KeyDown && event.key == Key.Menu -> { onLongPress(); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(10.dp)) {
            val iconSize = if (isSettings) 32 else 44
            if (app.icon != null) {
                Image(painter = remember(app.icon) { BitmapPainter(app.icon) }, contentDescription = app.label, modifier = Modifier.size(iconSize.dp))
            } else if (isSettings) {
                SettingsEmoji(app.label)
            } else {
                Text(app.label.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Light, color = AccentBlue)
            }
            Spacer(Modifier.height(6.dp))
            Text(app.label, style = TextStyle(fontSize = 11.sp, fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal, color = if (focused) TextW else TextSec, textAlign = TextAlign.Center, letterSpacing = 0.2.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingsEmoji(label: String) {
    Text(when { label.contains("Wi-Fi", true) -> "📶"; label.contains("Bluetooth", true) -> "◆"; label.contains("Display", true) -> "◻"; label.contains("Sound", true) -> "♪"; else -> "⚙" }, fontSize = 26.sp, color = TextSec)
}

// ─── Context Menu ──────────────────────────────────────────

@Composable
private fun ContextMenu(app: TvApp, isFavorite: Boolean, onDismiss: () -> Unit, onToggleFavorite: () -> Unit, onHide: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(onClick = onDismiss)
            .onKeyEvent { if (it.type == KeyEventType.KeyDown && it.key == Key.Back) { onDismiss(); true } else false },
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.width(260.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceFocused).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(app.label, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextW), modifier = Modifier.padding(vertical = 10.dp))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(TextTer.copy(alpha = 0.3f)))
            val favFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { try { favFocus.requestFocus() } catch (_: Exception) {} }
            MenuButton(text = if (isFavorite) "Remove from Favorites" else "Add to Favorites", modifier = Modifier.focusRequester(favFocus), onClick = onToggleFavorite)
            if (app.category != AppCategory.SETTINGS) { MenuButton(text = "Hide App", onClick = onHide) }
        }
    }
}

@Composable
private fun MenuButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(text, style = TextStyle(fontSize = 14.sp, color = if (focused) TextW else TextSec, textAlign = TextAlign.Center),
        modifier = modifier.fillMaxWidth().background(if (focused) AccentBlue.copy(alpha = 0.3f) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)
            .onKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.DirectionCenter)) { onClick(); true } else false }
            .padding(vertical = 12.dp, horizontal = 20.dp))
}

// ─── Quick Settings ────────────────────────────────────────

@Composable
fun QuickSettingsPanel(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val am = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    var volume by remember { mutableIntStateOf(am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)) }
    val maxVol = remember { am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) }
    var brightness by remember { mutableIntStateOf(try { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) } catch (_: Exception) { 128 }) }
    val canWrite = remember { Settings.System.canWrite(context) }

    fun adjBright(d: Int) {
        if (!canWrite) {
            try {
                val i = android.content.Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                i.data = android.net.Uri.parse("package:${context.packageName}")
                i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            } catch (_: Exception) {}
            return
        }
        brightness = (brightness + d).coerceIn(10, 255)
        try { Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness) } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).clickable(onClick = onDismiss)
            .onKeyEvent { if (it.type == KeyEventType.KeyDown && (it.key == Key.Back || it.key == Key.Menu)) { onDismiss(); true } else false },
        contentAlignment = Alignment.CenterEnd
    ) {
        val pf = remember { FocusRequester() }
        LaunchedEffect(Unit) { try { pf.requestFocus() } catch (_: Exception) {} }

        Column(
            modifier = Modifier.width(280.dp).padding(end = 32.dp).clip(RoundedCornerShape(20.dp)).background(Surface.copy(alpha = 0.95f)).padding(24.dp)
                .focusRequester(pf).focusable()
                .onKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown) when (e.key) {
                        Key.DirectionUp -> { val n = (volume + 1).coerceAtMost(maxVol); am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, n, 0); volume = n; true }
                        Key.DirectionDown -> { val n = (volume - 1).coerceAtLeast(0); am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, n, 0); volume = n; true }
                        Key.DirectionRight -> { adjBright(15); true }
                        Key.DirectionLeft -> { adjBright(-15); true }
                        Key.Back, Key.Menu -> { onDismiss(); true }
                        else -> false
                    } else false
                },
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Settings", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextW, letterSpacing = 0.5.sp))
            QsSlider("Volume", "${volume * 100 / maxVol.coerceAtLeast(1)}%", volume.toFloat() / maxVol.coerceAtLeast(1), "↑↓")
            QsSlider("Brightness", "${brightness * 100 / 255}%", brightness / 255f, "←→")
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(TextTer.copy(alpha = 0.2f)))
            QsActionButton("Screen Cast", "📺") { AppDiscovery.openCastSettings(context) }
            QsActionButton("Sleep", "💤") { onDismiss(); (context as? MainActivity)?.showScreensaver?.value = true }
            Text("Press Back to close", style = TextStyle(fontSize = 11.sp, color = TextTer, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun QsSlider(label: String, vt: String, v: Float, hint: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = TextStyle(fontSize = 13.sp, color = TextSec))
            Text(vt, style = TextStyle(fontSize = 13.sp, color = TextW, fontFamily = FontFamily.Monospace))
        }
        SliderBar(v); Text("$hint to adjust", style = TextStyle(fontSize = 10.sp, color = TextTer))
    }
}

@Composable
private fun QsActionButton(label: String, emoji: String, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (f) AccentBlue.copy(alpha = 0.2f) else Color.Transparent)
            .onFocusChanged { f = it.isFocused }.focusable().clickable(onClick = onClick)
            .onKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.DirectionCenter)) { onClick(); true } else false }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) { Text(emoji, fontSize = 18.sp); Text(label, style = TextStyle(fontSize = 14.sp, color = if (f) TextW else TextSec)) }
}

@Composable
private fun SliderBar(value: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF3A3A3C))) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(value.coerceIn(0f, 1f)).clip(RoundedCornerShape(2.dp)).background(TextW.copy(alpha = 0.8f)))
    }
}

// ─── Photo Screensaver ─────────────────────────────────────

@Composable
fun PhotoScreensaver(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var photoIds by remember { mutableStateOf(emptyList<Long>()) }
    var currentPhoto by remember { mutableStateOf<ImageBitmap?>(null) }
    var photoIndex by remember { mutableIntStateOf(0) }

    val drift = rememberInfiniteTransition(label = "d")
    val dx by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(25_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dx")
    val dy by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(19_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dy")

    LaunchedEffect(Unit) {
        photoIds = withContext(Dispatchers.IO) { getGalleryPhotoIds(context, 50) }
        if (photoIds.isNotEmpty()) {
            currentPhoto = withContext(Dispatchers.IO) { loadSinglePhoto(context, photoIds[0]) }
        }
    }

    LaunchedEffect(Unit) {
        val tf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val df = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        while (true) { val now = Date(); time = tf.format(now); date = df.format(now); delay(15_000) }
    }

    LaunchedEffect(photoIds) {
        if (photoIds.isNotEmpty()) {
            while (true) {
                delay(12_000)
                photoIndex = (photoIndex + 1) % photoIds.size
                val next = withContext(Dispatchers.IO) { loadSinglePhoto(context, photoIds[photoIndex]) }
                if (next != null) currentPhoto = next
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        currentPhoto?.let { photo ->
            Image(
                painter = remember(photo) { BitmapPainter(photo) },
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.3f },
                contentScale = ContentScale.Crop
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { translationX = (dx - 0.5f) * 400f; translationY = (dy - 0.5f) * 150f }
        ) {
            Text(time, style = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Thin, color = TextW.copy(alpha = 0.75f), letterSpacing = 4.sp))
            Text(date, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Light, color = TextW.copy(alpha = 0.35f), letterSpacing = 1.5.sp))
        }
    }
}

private fun getGalleryPhotoIds(context: android.content.Context, maxCount: Int): List<Long> {
    val ids = mutableListOf<Long>()
    try {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder) ?: return ids
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext() && ids.size < maxCount) { ids.add(it.getLong(idCol)) }
        }
    } catch (_: Exception) {}
    return ids
}

private fun loadSinglePhoto(context: android.content.Context, id: Long): ImageBitmap? {
    return try {
        val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        val input = context.contentResolver.openInputStream(imageUri) ?: return null
        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
        val bmp = BitmapFactory.decodeStream(input, null, opts)
        input.close()
        bmp?.asImageBitmap()
    } catch (_: Exception) { null }
}
