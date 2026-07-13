package com.binny.smarttv

import android.content.ContentUris
import android.provider.MediaStore
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// Apple-inspired dark palette
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
    resumeTick: Int = 0,
    onDismissScreensaver: () -> Unit,
    onDismissQuickSettings: () -> Unit
) {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf(emptyList<TvApp>()) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var recents by remember { mutableStateOf(emptyList<String>()) }
    var refreshTick by remember { mutableIntStateOf(0) }

    var contextApp by remember { mutableStateOf<TvApp?>(null) }

    LaunchedEffect(refreshTick, resumeTick) {
        allApps = try {
            withContext(Dispatchers.IO) { AppDiscovery.discoverApps(context) }
        } catch (e: Exception) {
            Log.e("MomTV", "Discovery failed", e)
            emptyList()
        }
        favorites = PrefsManager.getFavorites(context)
        recents = PrefsManager.getRecents(context)
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 28.dp, bottom = 12.dp)
        ) {
            TopBar()
            Spacer(modifier = Modifier.height(28.dp))
            AppGrid(
                allApps = allApps,
                favorites = favorites,
                recents = recents,
                onLaunch = { app ->
                    AppDiscovery.launchApp(context, app)
                    refreshTick++
                },
                onLongPress = { app -> contextApp = app }
            )
        }

        // Context menu
        contextApp?.let { app ->
            ContextMenu(
                app = app,
                isFavorite = app.packageName in favorites,
                onDismiss = { contextApp = null },
                onToggleFavorite = {
                    PrefsManager.toggleFavorite(context, app.packageName)
                    refreshTick++
                    contextApp = null
                },
                onHide = {
                    PrefsManager.toggleHidden(context, app.packageName)
                    refreshTick++
                    contextApp = null
                }
            )
        }

        // Quick settings
        AnimatedVisibility(
            visible = showQuickSettings,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            QuickSettingsPanel(onDismiss = onDismissQuickSettings)
        }

        // Screensaver
        AnimatedVisibility(
            visible = showScreensaver,
            enter = fadeIn(tween(1200)),
            exit = fadeOut(tween(400))
        ) {
            PhotoScreensaver(onDismiss = onDismissScreensaver)
        }
    }
}

// ─── Top Bar ───────────────────────────────────────────────

@Composable
private fun TopBar() {
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var greeting by remember { mutableStateOf("") }
    var weather by remember { mutableStateOf<WeatherData?>(null) }

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("h:mm", Locale.getDefault())
        val ampmFmt = SimpleDateFormat("a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        while (true) {
            val now = Date()
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            time = timeFmt.format(now)
            date = dateFmt.format(now)
            greeting = when {
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else -> "Good evening"
            }
            delay(15_000)
        }
    }

    LaunchedEffect(Unit) {
        weather = withContext(Dispatchers.IO) { WeatherService.fetchWeather() }
        while (true) {
            delay(30 * 60 * 1000L)
            weather = withContext(Dispatchers.IO) { WeatherService.fetchWeather() }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = greeting,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSec,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = date,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextTer
                )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            weather?.let { w ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = w.icon, fontSize = 18.sp)
                    Text(
                        text = w.tempDisplay,
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSec)
                    )
                }
            }
            Text(
                text = time,
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Thin,
                    color = TextW.copy(alpha = 0.9f),
                    letterSpacing = 2.sp
                )
            )
        }
    }
}

// ─── App Grid ──────────────────────────────────────────────

@Composable
private fun AppGrid(
    allApps: List<TvApp>,
    favorites: Set<String>,
    recents: List<String>,
    onLaunch: (TvApp) -> Unit,
    onLongPress: (TvApp) -> Unit
) {
    val appMap = remember(allApps) { allApps.associateBy { it.packageName } }
    val grouped = remember(allApps) { allApps.groupBy { it.category } }

    val favApps = remember(favorites, appMap) {
        favorites.mapNotNull { appMap[it] }
    }
    val recentApps = remember(recents, appMap, favorites) {
        recents.filter { it !in favorites }.mapNotNull { appMap[it] }
    }

    data class RowData(val key: String, val title: String, val apps: List<TvApp>, val isFirst: Boolean = false)

    val rows = remember(favApps, recentApps, grouped) {
        val r = mutableListOf<RowData>()
        if (favApps.isNotEmpty()) r.add(RowData("fav", "Favorites", favApps, isFirst = true))
        if (recentApps.isNotEmpty()) r.add(RowData("rec", "Recent", recentApps, isFirst = r.isEmpty()))
        val cats = listOf(AppCategory.WATCH, AppCategory.MUSIC, AppCategory.APPS, AppCategory.SETTINGS)
        for (cat in cats) {
            val apps = grouped[cat] ?: continue
            if (apps.isEmpty()) continue
            r.add(RowData(cat.name, cat.title, apps, isFirst = r.isEmpty()))
        }
        r
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(rows, key = { it.key }) { row ->
            AppRow(
                title = row.title,
                apps = row.apps,
                isFirst = row.isFirst,
                onLaunch = onLaunch,
                onLongPress = onLongPress
            )
        }
    }
}

@Composable
private fun AppRow(
    title: String,
    apps: List<TvApp>,
    isFirst: Boolean,
    onLaunch: (TvApp) -> Unit,
    onLongPress: (TvApp) -> Unit
) {
    val listState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }

    if (isFirst) {
        LaunchedEffect(Unit) {
            try { firstFocus.requestFocus() } catch (_: Exception) {}
        }
    }

    Column {
        Text(
            text = title.uppercase(),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTer,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(start = 48.dp, bottom = 10.dp)
        )

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps, key = { "${title}_${it.packageName}" }) { app ->
                val fr = remember { FocusRequester() }
                val mod = if (isFirst && app == apps.first()) {
                    Modifier.focusRequester(firstFocus)
                } else {
                    Modifier.focusRequester(fr)
                }
                AppCard(
                    app = app,
                    modifier = mod,
                    onLaunch = { onLaunch(app) },
                    onLongPress = { onLongPress(app) }
                )
            }
        }
    }
}

@Composable
private fun AppCard(
    app: TvApp,
    modifier: Modifier = Modifier,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "s"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (focused) 0.5f else 0f,
        animationSpec = tween(150),
        label = "b"
    )

    val isSettings = app.category == AppCategory.SETTINGS
    val w = if (isSettings) 110.dp else 130.dp
    val h = if (isSettings) 85.dp else 105.dp
    val iconSize = if (isSettings) 32 else 44

    Box(
        modifier = modifier
            .width(w)
            .height(h)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) SurfaceFocused else Surface)
            .then(
                if (focused) Modifier.border(1.5.dp, FocusGlow.copy(alpha = borderAlpha), RoundedCornerShape(16.dp))
                else Modifier
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onLaunch() },
                    onLongPress = { onLongPress() }
                )
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Enter, Key.DirectionCenter -> { onLaunch(); true }
                        Key.Menu -> { onLongPress(); true }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(10.dp)
        ) {
            if (app.icon != null) {
                Image(
                    painter = BitmapPainter(app.icon),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize.dp)
                )
            } else if (isSettings) {
                SettingsEmoji(app.label)
            } else {
                Text(
                    text = app.label.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = AccentBlue
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.label,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal,
                    color = if (focused) TextW else TextSec,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.2.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsEmoji(label: String) {
    val e = when {
        label.contains("Wi-Fi", true) -> "📶"
        label.contains("Bluetooth", true) -> "◆"
        label.contains("Display", true) -> "◻"
        label.contains("Sound", true) -> "♪"
        else -> "⚙"
    }
    Text(text = e, fontSize = 26.sp, color = TextSec)
}

// ─── Context Menu ──────────────────────────────────────────

@Composable
private fun ContextMenu(
    app: TvApp,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Back) { onDismiss(); true } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2C2C2E))
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = app.label,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextW),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            Box(Modifier.fillMaxWidth().height(0.5.dp).background(TextTer.copy(alpha = 0.3f)))

            val favFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { try { favFocus.requestFocus() } catch (_: Exception) {} }

            MenuButton(
                text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                modifier = Modifier.focusRequester(favFocus),
                onClick = onToggleFavorite
            )

            if (app.category != AppCategory.SETTINGS) {
                MenuButton(text = "Hide App", onClick = onHide)
            }
        }
    }
}

@Composable
private fun MenuButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text = text,
        style = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = if (focused) TextW else TextSec,
            textAlign = TextAlign.Center
        ),
        modifier = modifier
            .fillMaxWidth()
            .background(if (focused) AccentBlue.copy(alpha = 0.3f) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.DirectionCenter)) {
                    onClick(); true
                } else false
            }
            .padding(vertical = 12.dp, horizontal = 20.dp)
    )
}

// ─── Quick Settings ────────────────────────────────────────

@Composable
fun QuickSettingsPanel(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
    }

    var volume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC))
    }
    val maxVol = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) }

    var brightness by remember {
        mutableIntStateOf(
            try { android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) }
            catch (_: Exception) { 128 }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.Back || it.key == Key.Menu)) {
                    onDismiss(); true
                } else false
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        val panelFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { try { panelFocus.requestFocus() } catch (_: Exception) {} }

        Column(
            modifier = Modifier
                .width(280.dp)
                .padding(end = 32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E).copy(alpha = 0.95f))
                .padding(24.dp)
                .focusRequester(panelFocus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionUp -> {
                                val newVol = (volume + 1).coerceAtMost(maxVol)
                                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
                                volume = newVol; true
                            }
                            Key.DirectionDown -> {
                                val newVol = (volume - 1).coerceAtLeast(0)
                                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
                                volume = newVol; true
                            }
                            Key.DirectionRight -> {
                                brightness = (brightness + 15).coerceAtMost(255)
                                try {
                                    android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness)
                                } catch (_: Exception) {}
                                true
                            }
                            Key.DirectionLeft -> {
                                brightness = (brightness - 15).coerceAtLeast(10)
                                try {
                                    android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness)
                                } catch (_: Exception) {}
                                true
                            }
                            Key.Back, Key.Menu -> { onDismiss(); true }
                            else -> false
                        }
                    } else false
                },
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Settings",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextW, letterSpacing = 0.5.sp)
            )

            // Volume
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Volume", style = TextStyle(fontSize = 13.sp, color = TextSec))
                    Text("${(volume * 100 / maxVol.coerceAtLeast(1))}%", style = TextStyle(fontSize = 13.sp, color = TextW, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                }
                SliderBar(value = volume.toFloat() / maxVol.coerceAtLeast(1).toFloat())
                Text("↑↓ to adjust", style = TextStyle(fontSize = 10.sp, color = TextTer))
            }

            // Brightness
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Brightness", style = TextStyle(fontSize = 13.sp, color = TextSec))
                    Text("${(brightness * 100 / 255)}%", style = TextStyle(fontSize = 13.sp, color = TextW, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                }
                SliderBar(value = brightness.toFloat() / 255f)
                Text("←→ to adjust", style = TextStyle(fontSize = 10.sp, color = TextTer))
            }

            Box(Modifier.fillMaxWidth().height(0.5.dp).background(TextTer.copy(alpha = 0.2f)))

            Text(
                text = "Press Back to close",
                style = TextStyle(fontSize = 11.sp, color = TextTer, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SliderBar(value: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF3A3A3C))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(2.dp))
                .background(TextW.copy(alpha = 0.8f))
        )
    }
}

// ─── Photo Screensaver ─────────────────────────────────────

@Composable
fun PhotoScreensaver(onDismiss: () -> Unit) {
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    val drift = rememberInfiniteTransition(label = "d")
    val dx by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(25_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dx")
    val dy by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(19_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dy")

    LaunchedEffect(Unit) {
        val tf = SimpleDateFormat("h:mm", Locale.getDefault())
        val df = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        while (true) {
            val now = Date()
            time = tf.format(now)
            date = df.format(now)
            delay(15_000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(
                x = ((dx - 0.5f) * 180).dp,
                y = ((dy - 0.5f) * 80).dp
            )
        ) {
            Text(
                text = time,
                style = TextStyle(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Thin,
                    color = TextW.copy(alpha = 0.55f),
                    letterSpacing = 4.sp
                )
            )
            Text(
                text = date,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Light,
                    color = TextW.copy(alpha = 0.25f),
                    letterSpacing = 1.5.sp
                )
            )
        }
    }
}
