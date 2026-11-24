

==============================
ARCHIVO: ExoVideoPlayer.kt
RUTA: C:/Users/Maicol/AndroidStudioProjects/LatinoIPTVpro\app\src\main\java\com\example\latinoiptvpro\ui\components\ExoVideoPlayer.kt
==============================

@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.latinoiptvpro.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.provider.Settings
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.MediaCodecList
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import android.util.Rational
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.*
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.latinoiptvpro.ui.catalog.SharedCatalogViewModel
import com.example.latinoiptvpro.ui.m3u.M3uChannel
import com.example.latinoiptvpro.utils.AdBlockManager
import com.example.latinoiptvpro.utils.PC_USER_AGENT
import com.example.latinoiptvpro.utils.isTv
import com.example.latinoiptvpro.utils.SecurityProvider
import com.example.latinoiptvpro.utils.extractCombinedCookies
import com.example.latinoiptvpro.utils.extractors.ByseExtractor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

var globalIsPipMode by mutableStateOf(false)
var globalIsVideoPlaying by mutableStateOf(false)

const val ANDROID_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"

fun isAudioCodecSupported(mimeType: String): Boolean {
    if (mimeType.isBlank()) return false
    val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    for (codecInfo in codecList.codecInfos) {
        if (!codecInfo.isEncoder && codecInfo.supportedTypes.contains(mimeType)) {
            return true
        }
    }
    return false
}

sealed class ServerProfile {
    object GoodStream : ServerProfile()
    object Uqload : ServerProfile()
    object SmoothPre : ServerProfile()
    object Callistanise : ServerProfile()
    object Voe : ServerProfile()
    object VidLink : ServerProfile()
    object Odysee : ServerProfile()
    object MinoChinos : ServerProfile()
    object BysejikuartFamily : ServerProfile()
    object Fastream : ServerProfile()
    object Streamtape : ServerProfile()
    object Streamwish : ServerProfile()
    object SaveFiles : ServerProfile()
    object Regionales : ServerProfile()
    object Dailymotion : ServerProfile()
    object Nupload : ServerProfile()
    object Mixdrop : ServerProfile()
    object Vimeos : ServerProfile()
    object Mediafire : ServerProfile()
    object OkRu : ServerProfile()
    object Doodstream : ServerProfile()
    object MovieMaster : ServerProfile()
    object Unknown : ServerProfile()
}

data class ServerPlaybackConfig(
    val sendReferer: Boolean,
    val sendOrigin: Boolean,
    val sendCookies: Boolean,
    val sendSecFetchHeaders: Boolean,
    val useMobileUserAgent: Boolean = false,
    val userAgent: String? = null
)

// 🔑 Deriva las cookies de autorización de Mediafire desde el propio token.
//    URL = /{dkey+firma}/{qkey}/{archivo}  →  cookie "{qkey[0:2]}{dkey[0:2]}=1"
private fun mediafireAuthCookies(streamUrl: String): String {
    return try {
        val seg = android.net.Uri.parse(streamUrl).pathSegments
        if (seg.size < 2) return ""
        val dkey = seg[0]; val qkey = seg[1]
        if (dkey.length < 2 || qkey.length < 2) return ""
        "${qkey.take(2)}${dkey.take(2)}=1; dr_$qkey=1"
    } catch (_: Exception) { "" }
}

fun detectServer(streamUrl: String, pageUrl: String?): ServerProfile {
    val combined = (streamUrl + (pageUrl ?: "")).lowercase()
    return when {
        // ✅ MEDIAFIRE primero: el token aleatorio de ~200 chars puede contener
        // por azar "voe", "hls", "mixdrop"... y colarse en el perfil equivocado.
        combined.contains("mediafire.com") -> ServerProfile.Mediafire
        // ✅ OK.RU: el MP4 viene de mycdn.me (la URL del stream NO contiene "ok.ru"),
        // por eso también se detecta por el Referer de la página
        combined.contains("ok.ru") || combined.contains("mycdn.me") -> ServerProfile.OkRu
        // ✅ DOODSTREAM: el MP4 viene del CDN cloudatacdn.com/dood.video/doodcdn.io
        // La URL del stream NO contiene "dood" — se detecta por el CDN.
        combined.contains("cloudatacdn.com") || combined.contains("dood.video") ||
                combined.contains("doodcdn.io") || combined.contains("ds2play.com") ||
                combined.contains("playmogo.com") || combined.contains("doodstream.com") ||
                combined.contains("doply.net") -> ServerProfile.Doodstream
        // ✅ MOVIEMASTER: el m3u8 viene de api-encrypted.moviemaster.lat
        combined.contains("moviemaster.lat") -> ServerProfile.MovieMaster
        combined.contains("vimeos.zip") ||
                combined.contains("vimeos.net") ||
                combined.contains("vimeus.com") -> ServerProfile.Vimeos
        combined.contains("goodstream") -> ServerProfile.GoodStream
        combined.contains("uqload") -> ServerProfile.Uqload
        combined.contains("smoothpre") -> ServerProfile.SmoothPre
        combined.contains("callistanise") -> ServerProfile.Callistanise
        combined.contains("dhtpre") -> ServerProfile.Callistanise
        // ✅ VOE: el host rota entre espejos que NO contienen "voe" en el nombre.
        // También detectamos por el CDN del stream (cloudwindow-route.com/engine/hls2).
        combined.contains("voe") || combined.contains("jessicayeahcatch") ||
                combined.contains("lindalastattack") || combined.contains("caseyseemavoid") ||
                combined.contains("tubelessceliolymph") || combined.contains("simpulumlamerop") ||
                combined.contains("urochsunloath") || combined.contains("nathanfromsubject") ||
                combined.contains("yip.su") || combined.contains("metagnathtuggers") ||
                combined.contains("donaldlineelse") || combined.contains("crystaltreatmenteast") ||
                combined.contains("voeunblock") || combined.contains("voeunbl0ck") ||
                combined.contains("cloudwindow-route.com") -> ServerProfile.Voe
        combined.contains("vidlink") -> ServerProfile.VidLink
        combined.contains("odysee") -> ServerProfile.Odysee
        combined.contains("minochinos") -> ServerProfile.MinoChinos
        combined.contains("fastream") -> ServerProfile.Fastream
        combined.contains("streamtape") -> ServerProfile.Streamtape
        combined.contains("savefiles.com") -> ServerProfile.SaveFiles
        combined.contains("streamwish") || combined.contains("playnixes") || combined.contains("niramirus") ||
                combined.contains("premilkyway") || combined.contains("medixiru") || combined.contains("ghbrisk") ||
                combined.contains("awish.pro") || combined.contains("dwish.pro") || combined.contains("embedwish") ||
                combined.contains("wishembed") || combined.contains("kswplayer") || combined.contains("wishfast") ||
                combined.contains("sfastwish") || combined.contains("strwish") || combined.contains("flaswish") ||
                combined.contains("obeywish") || combined.contains("jodwish") || combined.contains("swhoi") ||
                combined.contains("multimovies.cloud") || combined.contains("uqloads") || combined.contains("cdnwish") ||
                combined.contains("asnwish") || combined.contains("neko-stream") || combined.contains("nekowish") ||
                combined.contains("swdyu") || combined.contains("wishonly") || combined.contains("playerwish") ||
                combined.contains("streamhls") || combined.contains("hlswish") || combined.contains("hgplaycdn") ->
            ServerProfile.Streamwish
        combined.contains("bysejikuart") ||
                combined.contains("bysesukior") ||
                combined.contains("cybervynx") ||
                combined.contains("owphbf24.com") -> ServerProfile.BysejikuartFamily
        combined.contains("regionales.saohgdasregions.fun") || combined.contains("saohgdasregions.fun") -> ServerProfile.Regionales
        combined.contains("dailymotion") -> ServerProfile.Dailymotion
        combined.contains("nupload") -> ServerProfile.Nupload
        combined.contains("dropcdn.io") || combined.contains("dr0pstream") || combined.contains("dropstream") -> ServerProfile.Streamwish
        combined.contains("mixdrop") || combined.contains("mxcontent.net") ||
                combined.contains("miiixdrop") || combined.contains("miixdrop") -> ServerProfile.Mixdrop
        combined.contains("cloudorchestranova") || combined.contains("moietyandmosaic") -> ServerProfile.Callistanise
        else -> ServerProfile.Unknown
    }
}

fun getConfigForServer(profile: ServerProfile): ServerPlaybackConfig {
    return when (profile) {
        is ServerProfile.Uqload, is ServerProfile.SmoothPre, is ServerProfile.Odysee ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = false, sendSecFetchHeaders = true)
        is ServerProfile.BysejikuartFamily ->
            ServerPlaybackConfig(
                sendReferer = true,
                sendOrigin = true,
                sendCookies = true,
                sendSecFetchHeaders = false,
                userAgent = "Mozilla/5.0 (Linux; Android 10; TX6s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36"
            )
        is ServerProfile.Callistanise ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = true, sendSecFetchHeaders = true)
        is ServerProfile.Voe ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = false, sendSecFetchHeaders = false)
        is ServerProfile.GoodStream ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = false, sendSecFetchHeaders = true)
        is ServerProfile.MinoChinos ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = true, sendSecFetchHeaders = true)
        is ServerProfile.Streamwish ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = false, sendSecFetchHeaders = true, useMobileUserAgent = true)
        is ServerProfile.SaveFiles ->
            ServerPlaybackConfig(
                sendReferer = true,
                sendOrigin = true,
                sendCookies = true,
                sendSecFetchHeaders = true,
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0"
            )
        is ServerProfile.Fastream, is ServerProfile.Streamtape, is ServerProfile.VidLink ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = false, sendSecFetchHeaders = true)
        is ServerProfile.Regionales ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = true, sendSecFetchHeaders = false)
        is ServerProfile.Dailymotion ->
            ServerPlaybackConfig(
                sendReferer = true,
                sendOrigin = true,
                sendCookies = false,
                sendSecFetchHeaders = false,
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/157.0.0.0 Safari/537.36"
            )
        is ServerProfile.Nupload ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = false, sendSecFetchHeaders = true)
        is ServerProfile.Mixdrop ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = true, sendSecFetchHeaders = true)
        is ServerProfile.Vimeos ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = true, sendSecFetchHeaders = false)
        // ✅ MEDIAFIRE: réplica del request que SÍ funcionó (Logcatklj.txt)
        //    Referer = la propia URL  →  Origin queda same-origin  →  200 OK
        is ServerProfile.Mediafire ->
            ServerPlaybackConfig(sendReferer = false, sendOrigin = false, sendCookies = false, sendSecFetchHeaders = false, useMobileUserAgent = false)
        is ServerProfile.OkRu ->
            ServerPlaybackConfig(
                sendReferer = true,
                sendOrigin = true,
                sendCookies = false,
                sendSecFetchHeaders = false,
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0"
            )
        // ✅ MOVIEMASTER: Referer = página original, sin cookies (el JWT es autocontenido)
        is ServerProfile.MovieMaster ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = false, sendSecFetchHeaders = false)
// ✅ DOODSTREAM: Referer = URL del embed (playmogo.com o espejo), sin cookies.
// UA Firefox como Cloudstream. El token+expiry son autocontenidos en la URL.
        is ServerProfile.Doodstream ->
            ServerPlaybackConfig(
                sendReferer = true,
                sendOrigin = true,
                sendCookies = false,
                sendSecFetchHeaders = false,
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0"
            )
        is ServerProfile.Unknown ->
            ServerPlaybackConfig(sendReferer = true, sendOrigin = true, sendCookies = true, sendSecFetchHeaders = false)
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
class DynamicHeadersDataSource(
    private val delegate: HttpDataSource,
    private val headersProvider: () -> Map<String, String>
) : HttpDataSource by delegate {
    override fun open(dataSpec: DataSpec): Long {
        val extraHeaders = headersProvider()
        val existing = dataSpec.httpRequestHeaders.toMutableMap()
        extraHeaders.forEach { (k, v) -> existing[k] = v }
        val newSpec = dataSpec.buildUpon().setHttpRequestHeaders(existing).build()
        return delegate.open(newSpec)
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
class LoggingHttpDataSource(
    private val delegate: HttpDataSource
) : HttpDataSource by delegate {
    override fun open(dataSpec: DataSpec): Long {
        val t0 = System.currentTimeMillis()
        return try {
            val result = delegate.open(dataSpec)
            Log.d("HTTP_TIMING", "✅ open() ${dataSpec.uri.host} en ${System.currentTimeMillis() - t0}ms")
            result
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            Log.d("HTTP_TIMING", "❌ open() ${dataSpec.uri.host} en ${System.currentTimeMillis() - t0}ms → HTTP ${e.responseCode}")
            if (dataSpec.uri.host?.contains("mediafire.com", true) == true) {
                Log.w("MF_403", "──── RESPUESTA DE MEDIAFIRE ────")
                e.headerFields?.forEach { (k, v) -> Log.w("MF_403", "  $k: $v") }
                val body = try { String(e.responseBody ?: ByteArray(0), Charsets.UTF_8).take(1500) } catch (_: Exception) { "" }
                if (body.isNotBlank()) Log.w("MF_403", "  BODY: $body")
                Log.w("MF_403", "──── HEADERS QUE ENViamos ────")
                dataSpec.httpRequestHeaders.forEach { (k, v) -> Log.w("MF_403", "  $k: ${v.take(90)}") }
            }
            throw e
        } catch (e: Exception) {
            Log.d("HTTP_TIMING", "❌ open() ${dataSpec.uri.host} en ${System.currentTimeMillis() - t0}ms → ${e.message}")
            throw e
        }
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
private fun createDataSourceFactory(
    resolvedUrl: String,
    currentHeaders: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>
): DataSource.Factory {
    return DataSource.Factory {
        val isWorkerUrl = resolvedUrl.contains("workers.dev", ignoreCase = true)
        val isMxContentUrl = resolvedUrl.contains("mxcontent.net", ignoreCase = true)
        val isDailymotionCdn =
            resolvedUrl.contains("dailymotion.com", ignoreCase = true) ||
                    resolvedUrl.contains("dmcdn.net", ignoreCase = true)

        val useOkHttp = isWorkerUrl || isMxContentUrl || isDailymotionCdn
        val base: HttpDataSource = if (useOkHttp) {
            val okHttpFactory = SecurityProvider.getOkHttpDataSourceFactory()
            okHttpFactory.createDataSource() as HttpDataSource
        } else {
            DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).createDataSource()
        }
        val dynamic = DynamicHeadersDataSource(base) { currentHeaders.toMap() }
        LoggingHttpDataSource(dynamic)
    }
}

@Suppress("DEPRECATION")
private fun Activity.hideSystemBarsCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.let { ctrl ->
            ctrl.hide(WindowInsets.Type.systemBars())
            ctrl.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}

@Suppress("DEPRECATION")
private fun Activity.showSystemBarsCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.show(WindowInsets.Type.systemBars())
    } else {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}

fun Modifier.tvFocusRing(cornerRadius: Dp): Modifier = composed {
    var hasFocus by remember { mutableStateOf(false) }
    this
        .onFocusChanged { hasFocus = it.isFocused }
        .border(
            width = if (hasFocus) 3.dp else 0.dp,
            color = if (hasFocus) Color.White else Color.Transparent,
            shape = RoundedCornerShape(cornerRadius)
        )
}

fun Activity.hideSystemBarsCompatExo() {
    hideSystemBarsCompat()
}

fun Activity.showSystemBarsCompatExo() {
    showSystemBarsCompat()
}

tailrec fun Context.findActivityForExo(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityForExo()
    else -> null
}

private fun getRealBrightness(activity: Activity?): Float {
    val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
    if (windowBrightness in 0f..1f) return windowBrightness
    return try {
        val currentActivity = activity ?: return 0.5f
        (Settings.System.getInt(
            currentActivity.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        ) / 255f).coerceIn(0.01f, 1f)
    } catch (_: Exception) {
        0.5f
    }
}

private fun hexToBytes(hex: String): ByteArray? {
    return try {
        if (hex.length % 2 != 0) return null
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    } catch (e: Exception) { null }
}

private fun bytesToBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

private fun extractClearKeyFromUrl(embedUrl: String): Pair<ByteArray, ByteArray>? {
    try {
        val uri = android.net.Uri.parse(embedUrl)
        val kParam = uri.getQueryParameter("k") ?: return null
        val decoded = String(Base64.decode(kParam, Base64.DEFAULT))
        val parts = decoded.split(":")
        if (parts.size == 2) {
            val keyIdBytes = hexToBytes(parts[0])
            val keyBytes = hexToBytes(parts[1])
            if (keyIdBytes != null && keyBytes != null) {
                return Pair(keyIdBytes, keyBytes)
            }
        }
    } catch (e: Exception) { }
    return null
}

@Composable
fun BrightnessBarVod(brightness: Float, modifier: Modifier = Modifier, showThumb: Boolean = false) {
    var trackHeight by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    Column(
        modifier = modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.BrightnessHigh, null, tint = Color.White, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.width(8.dp).fillMaxHeight(0.4f).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.3f)).onSizeChanged { size -> trackHeight = size.height.toFloat() }) {
            Box(modifier = Modifier.width(8.dp).fillMaxHeight(brightness.coerceIn(0.01f, 1f)).clip(RoundedCornerShape(4.dp)).background(Color.White).align(Alignment.BottomCenter))
            if (showThumb) {
                val thumbY = trackHeight * (1f - brightness.coerceIn(0.01f, 1f))
                Box(modifier = Modifier.size(12.dp).offset(y = with(density) { thumbY.toDp() } - 6.dp).align(Alignment.BottomCenter).clip(CircleShape).background(Color.White))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${(brightness * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun VolumeBarVod(volume: Float, modifier: Modifier = Modifier, showThumb: Boolean = false) {
    var trackHeight by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    Column(
        modifier = modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.White, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.width(8.dp).fillMaxHeight(0.4f).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.3f)).onSizeChanged { size -> trackHeight = size.height.toFloat() }) {
            Box(modifier = Modifier.width(8.dp).fillMaxHeight(volume.coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp)).background(Color.White).align(Alignment.BottomCenter))
            if (showThumb) {
                val thumbY = trackHeight * (1f - volume.coerceIn(0f, 1f))
                Box(modifier = Modifier.size(12.dp).offset(y = with(density) { thumbY.toDp() } - 6.dp).align(Alignment.BottomCenter).clip(CircleShape).background(Color.White))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${(volume * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun ExoFallbackWebView(
    url: String,
    channelName: String,
    onBackClick: (() -> Unit)?,
    isFullscreen: Boolean = false
) {
    val context = LocalContext.current
    val activity = context.findActivityForExo()
    val isTvDevice = remember { context.isTv() }

    fun restoreRequestedOrientation() {
        activity?.requestedOrientation = if (isTvDevice) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
    }

    val originalHost = try { android.net.Uri.parse(url).host } catch (_: Exception) { "" }

    var showControls by remember { mutableStateOf(true) }
    var controlsTimeout by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isMega = remember(url) { url.contains("mega.nz", ignoreCase = true) }

    val localAdBlockTerms = listOf(
        "doubleclick", "googleads", "adservice", "popads", "popunder",
        "exoclick", "adexchangerapid.com", "buttouuhuhggt.online",
        "push-sdk", "analytics", "tracking", "scorecardresearch",
        "quantserve", "histats", "dtscout", "mrktmtrcs",
        "onaudience", "tynt", "whos.amung.us", "blockadsnot",
        "adsco.re", "llvpn", "adbpage", "preferencenail",
        "protrafficinspector", "spendsdetachment", "zoologyfibre",
        "flushpersist", "kettledrooping", "realizationnewestfangs",
        "workdeadlinededicate", "convertedarchitecturepsychopath",
        "static.cloudflareinsights", "b7510", "usrpubtrk",
        "listenbutic", "dispone.clokeninesvindex", "bobapsoabauns.com", "waust.at", "fuckadblock",
        "064kk.com", "071kk.com", "bgtee.com", "tzegilo.com",
        "iimszqttinxcq.online", "adexchangeclear.com", "ikikigxywqcrf.site", "jnbhi.com"
    )

    fun triggerControlsTimeout() {
        showControls = true
        controlsTimeout = System.currentTimeMillis()
    }

    LaunchedEffect(controlsTimeout) {
        if (showControls) {
            delay(4000)
            if (System.currentTimeMillis() - controlsTimeout >= 3500) showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(0xFF000000.toInt())
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        if (isMega) { allowFileAccess = true; allowContentAccess = true }
                    }
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val duration = event.eventTime - event.downTime
                            if (duration < 300) {
                                showControls = !showControls
                                if (showControls) triggerControlsTimeout()
                            }
                        }
                        false
                    }
                    webChromeClient = object : WebChromeClient() {
                        private var customView: View? = null
                        private var customViewCallback: CustomViewCallback? = null
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (customView != null) { callback?.onCustomViewHidden(); return }
                            customView = view
                            customViewCallback = callback
                            val decorView = activity?.window?.decorView as? FrameLayout
                            decorView?.addView(customView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            activity?.hideSystemBarsCompat()
                        }
                        override fun onHideCustomView() {
                            val decorView = activity?.window?.decorView as? FrameLayout
                            decorView?.removeView(customView)
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            restoreRequestedOrientation()
                            activity?.showSystemBarsCompat()
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val newUrl = request?.url?.toString() ?: ""
                            if (newUrl.startsWith("intent://") || newUrl.startsWith("market://")) return true
                            if (newUrl == "about:blank") return true
                            val newHost = try { android.net.Uri.parse(newUrl).host } catch (_: Exception) { "" }
                            if (newHost != null && newHost != originalHost) return true
                            if (AdBlockManager.shouldBlock(newUrl) || localAdBlockTerms.any { newUrl.contains(it, ignoreCase = true) }) return true
                            return false
                        }
                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            val url = request?.url?.toString() ?: ""
                            if (AdBlockManager.shouldBlock(url) || localAdBlockTerms.any { url.contains(it, ignoreCase = true) }) {
                                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            val host = Uri.parse(url ?: "").host ?: ""
                            val cosmeticScript = AdBlockManager.getCosmeticScript(host)
                            if (cosmeticScript.isNotEmpty()) {
                                view?.evaluateJavascript(cosmeticScript, null)
                            }

                            view?.evaluateJavascript(
                                """
                                (function() {
                                    function unmuteAll() {
                                        var videos = document.querySelectorAll('video');
                                        videos.forEach(function(v) { try { v.muted = false; v.volume = 1.0; if (v.paused) v.play(); } catch(e) {} });
                                    }
                                    unmuteAll();
                                    var attempts = 0;
                                    var interval = setInterval(function() { unmuteAll(); if (++attempts >= 60) clearInterval(interval); }, 100);
                                    document.addEventListener('play', function(e) { if (e.target.tagName === 'VIDEO') { e.target.muted = false; e.target.volume = 1.0; } }, true);
                                })();
                                """.trimIndent(), null
                            )
                        }
                    }
                    val headers = mutableMapOf("Referer" to "https://www.tvporinternet2.com/")
                    if (isMega) headers.clear()
                    loadUrl(url, headers)
                }
            },
            update = { }
        )

        if (!globalIsPipMode) {
            AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent))).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        PlayerUiState.isFullscreen = false
                        restoreRequestedOrientation()
                        activity?.showSystemBarsCompat()
                        onBackClick?.invoke() ?: (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Salir del video", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = channelName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun isResolvedMediaUrl(value: String?): Boolean {
    val text = value?.trim().orEmpty()
    if (!(text.startsWith("http://") || text.startsWith("https://"))) return false
    if (text.contains("#EXTM3U", ignoreCase = true)) return false
    if (text.contains("#EXT-X-STREAM-INF", ignoreCase = true)) return false
    if (text.contains("VIDEO-RANGE=SDR", ignoreCase = true)) return false
    if (text.contains("\\n#EXT", ignoreCase = true)) return false

    val lower = text.lowercase()
    return lower.contains(".m3u8") ||
            lower.contains(".mp4") ||
            lower.contains(".mpd") ||
            lower.contains("/hls/") ||
            lower.contains("/stream/") ||
            lower.contains("master") ||
            lower.contains(".urlset/")
}

@Composable
fun ExoVideoPlayer(
    url: String,
    referer: String?,
    channelName: String,
    modifier: Modifier = Modifier,
    resumePositionMs: Long = 0L,
    preExtractedCookies: String = "",
    isSeries: Boolean = false,
    episodesList: List<M3uChannel> = emptyList(),
    currentEpisodeUrl: String = "",
    onEpisodeSelected: (String) -> Unit = {},
    onProgressUpdate: ((positionMs: Long, durationMs: Long) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onServerDead: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    onEpisodesClick: (() -> Unit)? = null,
    isEvent: Boolean = false,
    recoveryAttempt: Int = 0,
    originalEmbedUrl: String = "",
    seriesName: String = "",
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    serverName: String = "",
    sharedViewModel: SharedCatalogViewModel? = null,
    refererFallback: String = "",
    onHardHttpError: ((Int) -> Unit)? = null // ✅ NUEVO CALLBACK
) {
    if (url.contains("dailymotion", ignoreCase = true) && !isResolvedMediaUrl(url)) {
        val webViewUrl = originalEmbedUrl.ifBlank { referer?.takeIf { it.contains("geo.dailymotion.com") } ?: url }
        ExoFallbackWebView(url = webViewUrl, channelName = channelName, onBackClick = onBackClick, isFullscreen = PlayerUiState.isFullscreen)
        return
    }

    val isFastreamTokenStream = url.contains("fastream", ignoreCase = true) && url.contains(".m3u8", ignoreCase = true) && (url.contains("?t=") || url.contains("&t="))
    val isFullscreen = PlayerUiState.isFullscreen
    val isPipMode = globalIsPipMode

    if (isFastreamTokenStream && url.isNotBlank()) {
        ExoFallbackWebView(url = url, channelName = channelName, onBackClick = onBackClick, isFullscreen = isFullscreen)
        return
    }

    var fallbackToWebView by remember { mutableStateOf(false) }
    var drmFallback by remember { mutableStateOf<String?>(null) }

    var resolvedUrl by remember { mutableStateOf(url) }
    var isExtractingUrl by remember { mutableStateOf(false) }
    var cacheClearedAt90Percent by remember { mutableStateOf(false) }
    var showEpisodesPanel by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        fallbackToWebView = false
        drmFallback = null
        resolvedUrl = url
        cacheClearedAt90Percent = false
    }

    LaunchedEffect(url, sharedViewModel) {
        if (sharedViewModel != null &&
            sharedViewModel.isKnownEmbedServer(url) &&
            !isResolvedMediaUrl(url) &&
            !isEvent &&
            detectServer(url, referer) !is ServerProfile.OkRu &&
            detectServer(url, referer) !is ServerProfile.MovieMaster &&
            detectServer(url, referer) !is ServerProfile.Voe &&
            detectServer(url, referer) !is ServerProfile.Doodstream &&
            detectServer(url, referer) !is ServerProfile.Streamwish &&
            detectServer(url, referer) !is ServerProfile.SaveFiles &&
            !ByseExtractor.isByseUrl(url)) {
            isExtractingUrl = true
            val extracted = sharedViewModel.extractRealUrlFromEmbed(
                embedUrl = originalEmbedUrl.ifBlank { url },
                seriesName = seriesName,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                serverName = serverName
            )
            if (isResolvedMediaUrl(extracted)) {
                Log.d("ExoVideoPlayer", "✅ URL extraída: ${extracted!!.take(120)}...")
                resolvedUrl = extracted
            } else {
                Log.w("ExoVideoPlayer", "⚠️ Extractor devolvió un valor no reproducible; se conserva la URL original")
                resolvedUrl = url
                sharedViewModel.clearStreamUrlCache(
                    embedUrl = originalEmbedUrl.ifBlank { url },
                    seriesName = seriesName,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    serverName = serverName
                )
            }
            isExtractingUrl = false
        } else if (isResolvedMediaUrl(url)) {
            resolvedUrl = url
            isExtractingUrl = false
        }
    }

    if (fallbackToWebView && url.isNotBlank()) {
        ExoFallbackWebView(url = url, channelName = channelName, onBackClick = onBackClick, isFullscreen = isFullscreen)
        return
    }

    if (drmFallback != null) {
        ExoFallbackWebView(url = drmFallback!!, channelName = channelName, onBackClick = { drmFallback = null; onBackClick?.invoke() }, isFullscreen = isFullscreen)
        return
    }

    key(resolvedUrl) {
        val context = LocalContext.current
        val activity = context.findActivityForExo()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val currentHeaders = remember { mutableStateMapOf<String, String>() }

        val dataSourceFactory = remember {
            createDataSourceFactory(
                resolvedUrl = resolvedUrl,
                currentHeaders = currentHeaders
            )
        }

        val mediaSourceFactory = remember { DefaultMediaSourceFactory(dataSourceFactory).apply { setLiveTargetOffsetMs(5000L) } }

        val isMxContentStream = resolvedUrl.contains("mxcontent.net", ignoreCase = true)
        val exoPlayer = remember {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    if (isMxContentStream) 3000 else 10000,
                    if (isMxContentStream) 30000 else 60000,
                    if (isMxContentStream) 1000 else 2500,
                    if (isMxContentStream) 2000 else 5000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
                .build()
                .apply {
                    trackSelectionParameters = trackSelectionParameters.buildUpon().setPreferredAudioLanguages("es", "spa", "es-ES", "es-419", "lat", "español").build()
                    playWhenReady = true
                }
        }

        var playerResizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
        var audioBoostLevel by remember { mutableIntStateOf(0) }
        var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }
        val coroutineScope = rememberCoroutineScope()

        var availableQualities by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
        var selectedQualityHeight by remember { mutableIntStateOf(0) }
        var showQualityMenu by remember { mutableStateOf(false) }
        var showQualityToast by remember { mutableStateOf(false) }
        var qualityLabel by remember { mutableStateOf("Auto") }

        fun applyQuality(height: Int) {
            selectedQualityHeight = height
            val trackSelector = exoPlayer.trackSelector as? DefaultTrackSelector ?: return
            val builder = trackSelector.parameters.buildUpon()
            if (height == 0) {
                builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                qualityLabel = "Auto"
            } else {
                builder.setMaxVideoSize(height * 16 / 9, height)
                qualityLabel = when {
                    height >= 1080 -> "1080p"
                    height >= 720 -> "720p"
                    height >= 480 -> "480p"
                    height >= 360 -> "360p"
                    else -> "${height}p"
                }
            }
            trackSelector.setParameters(builder.build())
            showQualityToast = true
        }

        LaunchedEffect(showQualityToast) {
            if (showQualityToast) {
                delay(2000)
                showQualityToast = false
            }
        }

        val initialBrightness = remember { getRealBrightness(activity) }
        val initialVolume = remember {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) cur.toFloat() / max else 0f
        }
        var screenBrightness by remember { mutableFloatStateOf(initialBrightness) }
        var volumeLevel by remember { mutableFloatStateOf(initialVolume) }
        var showBrightnessBar by remember { mutableStateOf(false) }
        var showVolumeBar by remember { mutableStateOf(false) }

        val gestureController = remember {
            PlayerGestureController(
                activity = activity,
                audioManager = audioManager,
                onBrightnessChanged = { brightness -> screenBrightness = brightness },
                onVolumeChanged = { volume -> volumeLevel = volume },
                onHudStateChanged = { showBright, showVol -> showBrightnessBar = showBright; showVolumeBar = showVol },
                topBarrierDp = 60f
            )
        }

        var showNextEpisodeButton by remember { mutableStateOf(false) }
        var autoNextTriggered by remember { mutableStateOf(false) }
        var countdown by remember { mutableIntStateOf(20) }

        LaunchedEffect(showNextEpisodeButton) {
            if (showNextEpisodeButton && !autoNextTriggered) {
                countdown = 20
                while (countdown > 0 && !autoNextTriggered) {
                    delay(1000)
                    countdown--
                }
                if (countdown == 0 && !autoNextTriggered) {
                    autoNextTriggered = true
                    onNextEpisode?.invoke()
                }
            }
        }

        DisposableEffect(Unit) {
            globalIsVideoPlaying = true
            onDispose {
                globalIsVideoPlaying = false
                loudnessEnhancer?.release()
                loudnessEnhancer = null
                exoPlayer.release()
            }
        }

        var streamError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(resolvedUrl, referer, recoveryAttempt) {
            streamError = null
            showNextEpisodeButton = false
            autoNextTriggered = false

            val serverProfile = detectServer(resolvedUrl, referer)
            val config = getConfigForServer(serverProfile)
            val targetUserAgent = when {
                config.userAgent != null -> config.userAgent
                config.useMobileUserAgent -> ANDROID_USER_AGENT
                else -> PC_USER_AGENT
            }

            val newHeaders = mutableMapOf<String, String>()
            newHeaders["User-Agent"] = targetUserAgent

            var effectiveReferer = referer
            if (resolvedUrl.contains("cloudorchestranova", ignoreCase = true) || resolvedUrl.contains("moietyandmosaic", ignoreCase = true)) {
                effectiveReferer = "https://cloudorchestranova.com/"
            } else if (resolvedUrl.contains("sagaciousslumber.site", ignoreCase = true)) {
                effectiveReferer = "https://cloudorchestranova.com/"
            } else if (serverProfile is ServerProfile.Dailymotion && referer.isNullOrEmpty()) {
                effectiveReferer = "https://geo.dailymotion.com/"
            } else if (serverProfile is ServerProfile.Mixdrop && referer.isNullOrEmpty()) {
                effectiveReferer = "https://miiixdrop.net/"
            } else if (serverProfile is ServerProfile.Streamwish && referer.isNullOrEmpty() && url.contains("dropcdn.io", ignoreCase = true)) {
                effectiveReferer = "https://dr0pstream.com/"
            } else if (serverProfile is ServerProfile.Vimeos) {
                effectiveReferer = "https://vimeos.net/"
            } else if (serverProfile is ServerProfile.OkRu) {
                effectiveReferer = "https://ok.ru/"
            }
            if (config.sendReferer && !effectiveReferer.isNullOrEmpty()) {
                newHeaders["Referer"] = effectiveReferer
                try {
                    val refUri = java.net.URL(effectiveReferer)
                    if (config.sendOrigin) newHeaders["Origin"] = "${refUri.protocol}://${refUri.host}"
                } catch (_: Exception) {}
            }
            if (config.sendCookies) {
                val cookiesToUse = if (serverProfile is ServerProfile.Mediafire) {
                    // 🚨 FIX 403: las preExtractedCookies vienen de www.mediafire.com/file/...
                    // (sesión distinta a la del token). Recalculamos solo con la URL de descarga,
                    // exactamente como en la prueba manual que funcionó.
                    val cm = android.webkit.CookieManager.getInstance(); cm.flush()
                    extractCombinedCookies(streamUrl = resolvedUrl, embedPageUrl = resolvedUrl)
                } else if (preExtractedCookies.isNotBlank()) preExtractedCookies
                else {
                    val cm = android.webkit.CookieManager.getInstance(); cm.flush()
                    extractCombinedCookies(streamUrl = resolvedUrl, embedPageUrl = referer ?: resolvedUrl)
                }
                if (cookiesToUse.isNotBlank()) newHeaders["Cookie"] = cookiesToUse
            }
            newHeaders["Accept"] = "*/*"
            newHeaders["Accept-Language"] = "es-US,es-419;q=0.9,es;q=0.8"
            if (config.sendSecFetchHeaders) {
                newHeaders["Sec-Fetch-Site"] = "cross-site"
                newHeaders["Sec-Fetch-Mode"] = "cors"
                newHeaders["Sec-Fetch-Dest"] = "empty"
                newHeaders["Accept-Encoding"] = "gzip, deflate"
            }

            // ═══════════════════════════════════════════════════════════════
            // ✅ MEDIAFIRE: réplica EXACTA del request de Chrome (copia.txt)
            // ═══════════════════════════════════════════════════════════════
            if (serverProfile is ServerProfile.Mediafire) {
                newHeaders["Referer"] = "https://www.mediafire.com/"
                newHeaders.remove("Origin")   // Chrome NO manda Origin en navegación
                newHeaders["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
                newHeaders["Accept-Language"] = "es-US,es-419;q=0.9,es;q=0.8"
                newHeaders["Upgrade-Insecure-Requests"] = "1"
                newHeaders["Sec-Fetch-Site"] = "same-site"
                newHeaders["Sec-Fetch-Mode"] = "navigate"
                newHeaders["Sec-Fetch-User"] = "?1"
                newHeaders["Sec-Fetch-Dest"] = "document"

                val jar = try {
                    android.webkit.CookieManager.getInstance().flush()
                    extractCombinedCookies(resolvedUrl, "https://www.mediafire.com/")
                } catch (_: Exception) { "" }

                val map = LinkedHashMap<String, String>()
                fun putAll(raw: String) {
                    raw.split(";").forEach { p ->
                        val kv = p.trim().split("=", limit = 2)
                        if (kv.size == 2 && kv[0].isNotBlank()) map[kv[0].trim()] = kv[1].trim()
                    }
                }
                putAll(jar)
                val auth = mediafireAuthCookies(resolvedUrl)
                putAll(auth)   // 🔑 estas ganan: son las que autorizan el token
                if (map.isNotEmpty()) {
                    newHeaders["Cookie"] = map.entries.joinToString("; ") { "${it.key}=${it.value}" }
                }
                Log.d("MEDIAFIRE_HDR", "🔑 Auth derivada del token: $auth")
                Log.d("MEDIAFIRE_HDR", "🍪 Cookie final: ${newHeaders["Cookie"]?.length ?: 0} chars")
            }

            currentHeaders.clear()
            currentHeaders.putAll(newHeaders)
            if (serverProfile is ServerProfile.Mediafire) {
                Log.d("MEDIAFIRE_HDR", "🔎 Host=${android.net.Uri.parse(resolvedUrl).host}")
                Log.d("MEDIAFIRE_HDR", "🔎 Origin=${newHeaders["Origin"]}")
                Log.d("MEDIAFIRE_HDR", "🔎 Referer=${newHeaders["Referer"]?.take(70)}")
                Log.d("MEDIAFIRE_HDR", "🔎 Cookie=${if (newHeaders["Cookie"].isNullOrBlank()) "(vacía)" else "${newHeaders["Cookie"]!!.length} chars"}")
            }

            val isHlsStream = serverProfile !is ServerProfile.Mediafire &&
                    (resolvedUrl.contains(".m3u8", true) ||
                            resolvedUrl.contains("hls", true) ||
                            resolvedUrl.contains("master", true) ||
                            resolvedUrl.endsWith(".txt", true) ||
                            resolvedUrl.contains(".urlset", true) ||
                            resolvedUrl.contains("ibra.lat", true)) &&
                    !resolvedUrl.contains(".mpd", true)
            val isDashStream = resolvedUrl.contains(".mpd", true)
            val mediaItem = when {
                isHlsStream -> MediaItem.Builder().setUri(resolvedUrl).setMimeType(MimeTypes.APPLICATION_M3U8).build()
                isDashStream -> MediaItem.fromUri(resolvedUrl)
                // ✅ OK.RU: MP4 progresivo SIN extensión en la URL → declaramos el tipo
                // explícitamente, igual que hace Cloudstream (ExtractorLinkType.VIDEO)
                serverProfile is ServerProfile.OkRu -> MediaItem.Builder().setUri(resolvedUrl).setMimeType(MimeTypes.VIDEO_MP4).build()
                // ✅ MOVIEMASTER: HLS con token largo, MimeTypes declarado por seguridad
                serverProfile is ServerProfile.MovieMaster -> MediaItem.Builder().setUri(resolvedUrl).setMimeType(MimeTypes.APPLICATION_M3U8).build()
                // ✅ DOODSTREAM: MP4 con token largo, MimeTypes declarado por seguridad
                serverProfile is ServerProfile.Doodstream -> MediaItem.Builder().setUri(resolvedUrl).setMimeType(MimeTypes.VIDEO_MP4).build()
                // ✅ STREAMWISH: HLS desde espejo, MimeTypes declarado por seguridad
                serverProfile is ServerProfile.Streamwish -> MediaItem.Builder().setUri(resolvedUrl).setMimeType(MimeTypes.APPLICATION_M3U8).build()
                serverProfile is ServerProfile.SaveFiles -> MediaItem.Builder().setUri(resolvedUrl).setMimeType(MimeTypes.APPLICATION_M3U8).build()
                else -> MediaItem.fromUri(resolvedUrl)
            }

            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            val startPositionMs = if (resumePositionMs > 0L && !isEvent) resumePositionMs else 0L
            exoPlayer.setMediaSource(mediaSource, startPositionMs)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }

        var isPlaying by remember { mutableStateOf(true) }
        var isBuffering by remember { mutableStateOf(true) }
        var currentPos by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var bufferedPercentage by remember { mutableIntStateOf(0) }
        var showControls by remember { mutableStateOf(true) }
        var controlsTimeout by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var isDraggingSlider by remember { mutableStateOf(false) }
        var sliderPosition by remember { mutableFloatStateOf(0f) }
        var pendingSeek by remember { mutableLongStateOf(0L) }

        var audioTracks by remember { mutableStateOf<List<Triple<TrackGroup, Int, String>>>(emptyList()) }
        var subtitleTracks by remember { mutableStateOf<List<Triple<TrackGroup, Int, String>>>(emptyList()) }
        var currentAudioStr by remember { mutableStateOf("") }
        var currentSubStr by remember { mutableStateOf("Apagados") }

        val isLandscape = PlayerUiState.isFullscreen

        val lifecycleOwnerLocal = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwnerLocal, exoPlayer) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        if (activity?.isInPictureInPictureMode != true) exoPlayer.pause()
                        onProgressUpdate?.invoke(exoPlayer.currentPosition.coerceAtLeast(0L), exoPlayer.duration.takeIf { it > 0 } ?: 0L)
                    }
                    Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                    Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                    else -> {}
                }
            }
            lifecycleOwnerLocal.lifecycle.addObserver(observer)
            onDispose {
                onProgressUpdate?.invoke(exoPlayer.currentPosition.coerceAtLeast(0L), exoPlayer.duration.takeIf { it > 0 } ?: 0L)
                lifecycleOwnerLocal.lifecycle.removeObserver(observer)
            }
        }

        fun triggerControlsTimeout() { showControls = true; controlsTimeout = System.currentTimeMillis() }

        LaunchedEffect(controlsTimeout, isPlaying) {
            if (showControls && isPlaying) {
                delay(4000)
                if (System.currentTimeMillis() - controlsTimeout >= 3500) showControls = false
            }
        }

        LaunchedEffect(audioBoostLevel, loudnessEnhancer) {
            try {
                val gainMB = when (audioBoostLevel) { 1 -> 350; 2 -> 600; else -> 0 }
                loudnessEnhancer?.setTargetGain(gainMB)
            } catch (e: Exception) { }
        }

        LaunchedEffect(exoPlayer) {
            var lastSaveTime = System.currentTimeMillis()
            while (true) {
                if (exoPlayer.isPlaying && !isDraggingSlider) {
                    currentPos = exoPlayer.currentPosition
                    duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                    bufferedPercentage = exoPlayer.bufferedPercentage

                    if (sharedViewModel != null && duration > 0 && currentPos > 0 && !cacheClearedAt90Percent) {
                        val progressPercent = (currentPos.toFloat() / duration.toFloat()) * 100f
                        if (progressPercent >= 90f) {
                            Log.d("ExoVideoPlayer", "🔄 Video al 90%, limpiando caché de: ${url.take(50)}...")
                            sharedViewModel.clearStreamUrlCache(
                                embedUrl = originalEmbedUrl.ifBlank { url },
                                seriesName = seriesName,
                                seasonNumber = seasonNumber,
                                episodeNumber = episodeNumber,
                                serverName = serverName
                            )
                            cacheClearedAt90Percent = true
                        }
                    }

                    if (isSeries && duration > 0 && onNextEpisode != null && !autoNextTriggered) {
                        val remainingMs = duration - currentPos
                        if (remainingMs <= 20_000L && !showNextEpisodeButton) {
                            showNextEpisodeButton = true
                        }
                        if (remainingMs <= 0L && showNextEpisodeButton) {
                            autoNextTriggered = true
                            showNextEpisodeButton = false
                            onNextEpisode.invoke()
                        }
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastSaveTime >= 10000) {
                        onProgressUpdate?.invoke(currentPos, duration)
                        lastSaveTime = now
                    }
                }
                delay(500)
            }
        }

        DisposableEffect(exoPlayer) {
            var isManualAudioForced = false
            val listener = object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (error.cause is HlsPlaylistTracker.PlaylistStuckException) return
                    var httpError: HttpDataSource.InvalidResponseCodeException? = null
                    var cause: Throwable? = error
                    while (cause != null) { if (cause is HttpDataSource.InvalidResponseCodeException) { httpError = cause; break }; cause = cause.cause }
                    if (httpError != null) {
                        val code = httpError.responseCode
                        if (code == 403 || code == 401 || code == 404) {
                            // ✅ Mediafire: un 403 es problema de headers, NO servidor caído.
                            // No contaminar Supabase con falsos positivos.
                            if (detectServer(resolvedUrl, referer) !is ServerProfile.Mediafire) {
                                onHardHttpError?.invoke(code)
                            } else {
                                Log.w("MEDIAFIRE_HDR", "⚠️ 403 en Mediafire → NO se reporta como servidor caído")
                            }

                            val currentRef = currentHeaders["Referer"] ?: ""
                            if (refererFallback.isNotBlank() && !currentRef.equals(refererFallback, ignoreCase = true)) {
                                Log.d("ExoVideoPlayer", "🔄 Error $code. Reintentando con refererFallback: $refererFallback")
                                currentHeaders["Referer"] = refererFallback
                                try {
                                    val refUri = java.net.URL(refererFallback)
                                    currentHeaders["Origin"] = "${refUri.protocol}://${refUri.host}"
                                } catch (_: Exception) {}
                                coroutineScope.launch {
                                    delay(300)
                                    val cp = exoPlayer.currentPosition
                                    exoPlayer.prepare()
                                    if (cp > 0) exoPlayer.seekTo(cp)
                                    exoPlayer.play()
                                }
                                return
                            }
                            val refreshEmbedUrl = originalEmbedUrl.ifBlank { url }
                            if (sharedViewModel != null &&
                                sharedViewModel.isKnownEmbedServer(refreshEmbedUrl) &&
                                !isResolvedMediaUrl(refreshEmbedUrl) &&
                                !isEvent) {
                                coroutineScope.launch {
                                    Log.d("ExoVideoPlayer", "🔄 Error $code, renovando URL del embed original...")
                                    sharedViewModel.clearStreamUrlCache(
                                        embedUrl = refreshEmbedUrl,
                                        seriesName = seriesName,
                                        seasonNumber = seasonNumber,
                                        episodeNumber = episodeNumber,
                                        serverName = serverName
                                    )
                                    val newUrl = sharedViewModel.extractRealUrlFromEmbed(
                                        embedUrl = refreshEmbedUrl,
                                        seriesName = seriesName,
                                        seasonNumber = seasonNumber,
                                        episodeNumber = episodeNumber,
                                        serverName = serverName
                                    )
                                    if (newUrl != null && newUrl != resolvedUrl) {
                                        Log.d("ExoVideoPlayer", "✅ Nueva URL obtenida, reintentando...")
                                        resolvedUrl = newUrl
                                    } else {
                                        if (isEvent) onServerDead?.invoke() else { streamError = "Error al reproducir este contenido ($code)"; exoPlayer.stop() }
                                    }
                                }
                                return
                            }
                            if (isEvent) onServerDead?.invoke() else { streamError = "Error al reproducir este contenido ($code)"; exoPlayer.stop() }
                            return
                        }
                        if (code in 500..599) {
                            coroutineScope.launch { delay(2000); val cp = exoPlayer.currentPosition; exoPlayer.prepare(); exoPlayer.seekTo(cp); exoPlayer.play() }
                            return
                        }
                    }
                    if (error.errorCode == PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED || error.errorCode == PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR || error.errorCode == PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED) {
                        if (isEvent) onServerDead?.invoke() else { if (originalEmbedUrl.isNotBlank()) { drmFallback = originalEmbedUrl; exoPlayer.stop() } else { streamError = "Contenido protegido con DRM no soportado"; exoPlayer.stop() } }
                        return
                    }
                    if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED || error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT) {
                        coroutineScope.launch { delay(1500); val cp = exoPlayer.currentPosition; exoPlayer.prepare(); exoPlayer.seekTo(cp); exoPlayer.play() }
                        return
                    }
                    Log.e("EXO_RECOVERY", "Error irrecuperable (${error.errorCodeName}).")
                    if (isEvent) onServerDead?.invoke()
                    else if (resolvedUrl.contains("workers.dev") || resolvedUrl.contains(".mp4") && !resolvedUrl.contains("m3u8")) { streamError = "⚠️ Este contenido no está disponible en este momento."; exoPlayer.stop() }
                    else fallbackToWebView = true
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                        if (pendingSeek > 0L) { exoPlayer.seekTo(pendingSeek); pendingSeek = 0L }
                    }
                }
                override fun onIsPlayingChanged(isPlayingState: Boolean) { isPlaying = isPlayingState }
                override fun onTracksChanged(tracks: Tracks) {
                    val aTracks = mutableListOf<Triple<TrackGroup, Int, String>>()
                    val sTracks = mutableListOf<Triple<TrackGroup, Int, String>>()
                    val qualities = mutableListOf<Pair<String, Int>>()
                    for (group in tracks.groups) {
                        if (group.type == C.TRACK_TYPE_AUDIO) {
                            for (i in 0 until group.length) {
                                val format = group.mediaTrackGroup.getFormat(i)
                                val label = format.language ?: format.label ?: "Audio ${aTracks.size + 1}"
                                aTracks.add(Triple(group.mediaTrackGroup, i, label.uppercase()))
                                if (group.isTrackSelected(i)) currentAudioStr = label.uppercase()
                            }
                        } else if (group.type == C.TRACK_TYPE_TEXT) {
                            for (i in 0 until group.length) {
                                val format = group.mediaTrackGroup.getFormat(i)
                                val label = format.language ?: format.label ?: "Sub ${sTracks.size + 1}"
                                sTracks.add(Triple(group.mediaTrackGroup, i, label.uppercase()))
                                if (group.isTrackSelected(i)) currentSubStr = label.uppercase()
                            }
                        } else if (group.type == C.TRACK_TYPE_VIDEO) {
                            for (i in 0 until group.length) {
                                val format = group.mediaTrackGroup.getFormat(i)
                                if (format.height > 0) {
                                    val label = when {
                                        format.height >= 1080 -> "1080p"
                                        format.height >= 720 -> "720p"
                                        format.height >= 480 -> "480p"
                                        format.height >= 360 -> "360p"
                                        else -> "${format.height}p"
                                    }
                                    qualities.add(Pair(label, format.height))
                                }
                            }
                        }
                    }
                    audioTracks = aTracks; subtitleTracks = sTracks
                    availableQualities = qualities.distinctBy { it.first }.sortedByDescending { it.second }
                    val isTextDisabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                    if (isTextDisabled) currentSubStr = "Apagados"
                    if (!isManualAudioForced && aTracks.isNotEmpty()) {
                        var found = false
                        for (group in tracks.groups) {
                            if (group.type == C.TRACK_TYPE_AUDIO) {
                                for (i in 0 until group.length) {
                                    val format = group.mediaTrackGroup.getFormat(i)
                                    val codec = format.sampleMimeType ?: ""
                                    val tl = (format.label ?: format.language ?: "").lowercase()
                                    val isSpanish = tl.contains("lat") || tl.contains("spa") || tl.contains("es") || tl.contains("español")
                                    if (isSpanish && isAudioCodecSupported(codec)) {
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i))).build()
                                        found = true; break
                                    }
                                }
                                if (found) break
                            }
                        }
                        if (!found) {
                            for (group in tracks.groups) {
                                if (group.type == C.TRACK_TYPE_AUDIO) {
                                    for (i in 0 until group.length) {
                                        val codec = group.mediaTrackGroup.getFormat(i).sampleMimeType ?: ""
                                        if (isAudioCodecSupported(codec)) {
                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i))).build()
                                            found = true; break
                                        }
                                    }
                                    if (found) break
                                }
                            }
                        }
                        isManualAudioForced = true
                    }
                }
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                        try {
                            loudnessEnhancer?.release()
                            val enhancer = LoudnessEnhancer(audioSessionId)
                            enhancer.setTargetGain(when (audioBoostLevel) { 1 -> 350; 2 -> 600; else -> 0 })
                            enhancer.enabled = true
                            loudnessEnhancer = enhancer
                        } catch (e: Exception) { }
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose { exoPlayer.removeListener(listener) }
        }

        var playerSize by remember { mutableStateOf(IntSize.Zero) }

        // 📺 FocusRequesters para TV
        val isTvDevice = remember { context.isTv() }

        fun restoreRequestedOrientation() {
            activity?.requestedOrientation = if (isTvDevice) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        }

        val surfaceFocusRequester = remember { FocusRequester() }
        val playFocusRequester = remember { FocusRequester() }
        val seekFocusRequester = remember { FocusRequester() }
        val episodesButtonFocusRequester = remember { FocusRequester() }
        val episodesCloseFocusRequester = remember { FocusRequester() }
        val currentEpisodeFocusRequester = remember { FocusRequester() }
        var restoreEpisodesButtonFocus by remember { mutableStateOf(false) }
        var surfaceHasFocus by remember { mutableStateOf(false) }
        var playHasFocus by remember { mutableStateOf(false) }
        var seekBarHasFocus by remember { mutableStateOf(false) }
        var seekDirection by remember { mutableIntStateOf(0) }

        fun finishDpadSeek() {
            if (seekDirection == 0) return
            seekDirection = 0
            val maxPosition = duration.takeIf { it > 0L } ?: Long.MAX_VALUE
            val target = sliderPosition.toLong().coerceIn(0L, maxPosition)
            isDraggingSlider = false
            currentPos = target
            exoPlayer.seekTo(target)
            exoPlayer.playWhenReady = true
            triggerControlsTimeout()
        }

        fun beginDpadSeek(direction: Int) {
            if (seekDirection != direction) {
                val maxPosition = duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                val current = if (isDraggingSlider) sliderPosition.toLong() else exoPlayer.currentPosition.coerceAtLeast(0L)
                if (seekDirection == 0) exoPlayer.pause()
                sliderPosition = (current + direction * 10_000L).coerceIn(0L, maxPosition).toFloat()
                isDraggingSlider = true
                seekDirection = direction
            }
            triggerControlsTimeout()
        }

        fun handleSeekButtonKey(native: android.view.KeyEvent, direction: Int): Boolean {
            if (native.keyCode != android.view.KeyEvent.KEYCODE_DPAD_CENTER &&
                native.keyCode != android.view.KeyEvent.KEYCODE_ENTER &&
                native.keyCode != android.view.KeyEvent.KEYCODE_NUMPAD_ENTER) return false
            return when (native.action) {
                android.view.KeyEvent.ACTION_DOWN -> { beginDpadSeek(direction); true }
                android.view.KeyEvent.ACTION_UP -> {
                    if (seekDirection == direction) finishDpadSeek()
                    true
                }
                else -> false
            }
        }

        fun exitVideoPlayback() {
            seekDirection = 0
            isDraggingSlider = false
            exoPlayer.pause()
            PlayerUiState.isFullscreen = false
            restoreRequestedOrientation()
            activity?.showSystemBarsCompat()
            onBackClick?.invoke() ?: (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
        }

        fun closeEpisodesPanel() {
            restoreEpisodesButtonFocus = isTvDevice
            showEpisodesPanel = false
        }

        BackHandler(enabled = showEpisodesPanel && isSeries) {
            closeEpisodesPanel()
        }

        LaunchedEffect(seekDirection) {
            if (seekDirection == 0) return@LaunchedEffect
            while (seekDirection != 0) {
                delay(120L)
                val direction = seekDirection
                if (direction == 0) break
                val maxPosition = duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                val target = (sliderPosition.toLong() + direction * 2_000L)
                    .coerceIn(0L, maxPosition)
                sliderPosition = target.toFloat()
                triggerControlsTimeout()
            }
        }

        // 📺 Auto-enfocar según el estado de los controles
        LaunchedEffect(showControls, showEpisodesPanel, isTvDevice, isBuffering, streamError) {
            if (!isTvDevice) return@LaunchedEffect
            if (showEpisodesPanel) return@LaunchedEffect
            if (restoreEpisodesButtonFocus) {
                delay(120)
                runCatching { episodesButtonFocusRequester.requestFocus() }
                restoreEpisodesButtonFocus = false
                return@LaunchedEffect
            }
            if (!showControls) {
                runCatching { surfaceFocusRequester.requestFocus() }
            } else if (!isBuffering && streamError == null) {
                delay(300)
                runCatching { playFocusRequester.requestFocus() }
            }
        }

        Box(
            modifier = modifier
                .background(Color.Black)
                .focusRequester(surfaceFocusRequester)
                .onFocusChanged { surfaceHasFocus = it.isFocused }
                .onPreviewKeyEvent { event ->
                    val native = event.nativeKeyEvent
                    val mediaSeekDirection = when (native.keyCode) {
                        android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                        android.view.KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> 1
                        android.view.KeyEvent.KEYCODE_MEDIA_REWIND,
                        android.view.KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> -1
                        else -> 0
                    }
                    if (mediaSeekDirection != 0) {
                        when (native.action) {
                            android.view.KeyEvent.ACTION_DOWN -> {
                                beginDpadSeek(mediaSeekDirection)
                                true
                            }
                            android.view.KeyEvent.ACTION_UP -> {
                                if (seekDirection == mediaSeekDirection) finishDpadSeek()
                                true
                            }
                            else -> false
                        }
                    } else {
                        if (native.action != android.view.KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                        when (native.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                when {
                                    showEpisodesPanel -> false  // deja que el panel maneje su foco
                                    !showControls -> { triggerControlsTimeout(); true }
                                    surfaceHasFocus -> { runCatching { playFocusRequester.requestFocus() }; true }
                                    else -> false
                                }
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                when {
                                    showEpisodesPanel -> false
                                    !showControls -> { triggerControlsTimeout(); true }
                                    isTvDevice && playHasFocus -> {
                                        runCatching { seekFocusRequester.requestFocus() }
                                        true
                                    }
                                    else -> false
                                }
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_UP,
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (!showControls && !showEpisodesPanel) { triggerControlsTimeout(); true } else false
                            }
                            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                triggerControlsTimeout(); true
                            }
                            android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                exoPlayer.play(); triggerControlsTimeout(); true
                            }
                            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                exoPlayer.pause(); triggerControlsTimeout(); true
                            }
                            android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                if (isSeries) { onNextEpisode?.invoke(); true } else false
                            }
                            else -> false
                        }
                    }
                }
                .focusable()
        ) {
            if (isExtractingUrl) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFE50914), strokeWidth = 4.dp, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Extrayendo video...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false
                        controllerAutoShow = false
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        this.resizeMode = playerResizeMode
                    }
                },
                update = { view -> view.resizeMode = playerResizeMode },
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged { playerSize = it }
                    .pointerInput(isLandscape, isTvDevice) {
                        // 📺 En TV no hay gestos táctiles: todo va por D-pad
                        if (isTvDevice) return@pointerInput
                        var startPosition = Offset.Zero
                        var isGesture = false
                        val tapThreshold = 12.dp.toPx()
                        val topBarrierPx = 60.dp.toPx()
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val position = event.changes.firstOrNull()?.position ?: continue
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        startPosition = position
                                        isGesture = false
                                        if (isLandscape && position.y > topBarrierPx) {
                                            gestureController.onDown(position, playerSize.height.toFloat())
                                        }
                                    }
                                    PointerEventType.Move -> {
                                        val distance = (position - startPosition).getDistance()
                                        if (!isGesture && distance > tapThreshold) isGesture = true
                                        if (isLandscape && isGesture && startPosition.y > topBarrierPx) {
                                            gestureController.onMove(position)
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        if (!isGesture && !isPipMode && startPosition.y > topBarrierPx) {
                                            showControls = !showControls
                                            if (showControls) triggerControlsTimeout()
                                        }
                                        gestureController.onUp()
                                    }
                                }
                            }
                        }
                    }
            )

            if (isBuffering) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFE50914), strokeWidth = 4.dp, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("$bufferedPercentage %", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (streamError != null) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Text(text = streamError ?: "", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
                }
            }

            if (showQualityToast) {
                AnimatedVisibility(
                    visible = showQualityToast,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Calidad: $qualityLabel", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 📺 En TV el brillo lo maneja el sistema, el volumen también → no mostrar barras
            if (!isTvDevice && isLandscape && showBrightnessBar && !isPipMode) {
                BrightnessBarVod(brightness = screenBrightness, modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp), showThumb = true)
            }
            if (!isTvDevice && isLandscape && showVolumeBar && !isPipMode) {
                VolumeBarVod(volume = volumeLevel, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp), showThumb = true)
            }

            if (isSeries && showNextEpisodeButton && !autoNextTriggered && !isPipMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .tvFocusRing(cornerRadius = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { showNextEpisodeButton = false; autoNextTriggered = true; onNextEpisode?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Siguiente capítulo", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Reproduciendo en $countdown...", color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { showNextEpisodeButton = false; autoNextTriggered = true }) { Text("Cancelar", color = Color.Gray) }
                }
            }

            if (!isPipMode) {
                AnimatedVisibility(visible = showControls && streamError == null, enter = fadeIn(), exit = fadeOut()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))))
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))

                        if (isSeries && isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, top = 24.dp, end = 16.dp, bottom = 16.dp)
                                    .align(Alignment.TopStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val backInteraction = remember { MutableInteractionSource() }
                                val backPressed by backInteraction.collectIsPressedAsState()
                                val backScale by animateFloatAsState(if (backPressed) 0.85f else 1f, label = "backScale")

                                Box(
                                    modifier = Modifier
                                        .tvFocusRing(cornerRadius = 24.dp)
                                        .size(48.dp)
                                        .scale(backScale)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = if (backPressed) 0.8f else 0.4f))
                                        .clickable(
                                            interactionSource = backInteraction,
                                            indication = null
                                        ) {
                                            exitVideoPlayback()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Salir del video", tint = Color.White, modifier = Modifier.size(24.dp))
                                }

                                Spacer(Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "S${seasonNumber ?: 1}:E${episodeNumber ?: 1}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = channelName,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.weight(1f))

                                IconButton(onClick = {
                                    playerResizeMode = when (playerResizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                    triggerControlsTimeout()
                                }, modifier = Modifier.tvFocusRing(cornerRadius = 20.dp).size(36.dp)) {
                                    val icon = when (playerResizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> Icons.Default.AspectRatio
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> Icons.Default.ZoomIn
                                        else -> Icons.Default.Fullscreen
                                    }
                                    Icon(icon, "Relación de aspecto", tint = Color.White, modifier = Modifier.size(26.dp))
                                }

                                Spacer(Modifier.width(16.dp))

                                Box {
                                    IconButton(onClick = { showQualityMenu = true; triggerControlsTimeout() }, modifier = Modifier.tvFocusRing(cornerRadius = 20.dp).size(36.dp)) {
                                        Icon(Icons.Default.Hd, "Calidad", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                    DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }, modifier = Modifier.background(Color(0xFF2B2B2B))) {
                                        DropdownMenuItem(
                                            text = { Text("Auto", color = if (selectedQualityHeight == 0) Color(0xFFE50914) else Color.White, fontWeight = FontWeight.Bold) },
                                            onClick = { applyQuality(0); showQualityMenu = false }
                                        )
                                        availableQualities.forEach { quality ->
                                            val isSel = selectedQualityHeight == quality.second
                                            DropdownMenuItem(
                                                text = { Text(quality.first, color = if (isSel) Color(0xFFE50914) else Color.White) },
                                                onClick = { applyQuality(quality.second); showQualityMenu = false }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(16.dp))

                                if (!isTvDevice) {
                                    IconButton(onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
                                            activity?.enterPictureInPictureMode(params)
                                        }
                                    }, modifier = Modifier.tvFocusRing(cornerRadius = 20.dp).size(36.dp)) {
                                        Icon(Icons.Default.PictureInPictureAlt, "PiP Mode", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                }
                            }

                            if (!isBuffering) {
                                Row(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val backInteraction = remember { MutableInteractionSource() }
                                    val backPressed by backInteraction.collectIsPressedAsState()
                                    val backScale by animateFloatAsState(if (backPressed) 0.85f else 1f, label = "backScale")

                                    Box(
                                        modifier = Modifier
                                            .tvFocusRing(cornerRadius = 28.dp)
                                            .size(56.dp)
                                            .scale(backScale)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = if (backPressed) 0.8f else 0.5f))
                                            .onPreviewKeyEvent { handleSeekButtonKey(it.nativeKeyEvent, -1) }
                                            .clickable(
                                                interactionSource = backInteraction,
                                                indication = null
                                            ) {
                                                exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                                                triggerControlsTimeout()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(32.dp))
                                    }

                                    Spacer(Modifier.width(if (isLandscape) 80.dp else 40.dp))

                                    val playInteraction = remember { MutableInteractionSource() }
                                    val playPressed by playInteraction.collectIsPressedAsState()
                                    val playScale by animateFloatAsState(if (playPressed) 0.85f else 1f, label = "playScale")

                                    Box(
                                        modifier = Modifier
                                            .focusRequester(playFocusRequester)
                                            .onFocusChanged { playHasFocus = it.isFocused }
                                            .tvFocusRing(cornerRadius = 40.dp)
                                            .size(80.dp)
                                            .scale(playScale)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = if (playPressed) 0.8f else 0.5f))
                                            .clickable(
                                                interactionSource = playInteraction,
                                                indication = null
                                            ) {
                                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                                triggerControlsTimeout()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(if (isLandscape) 80.dp else 40.dp))

                                    val forwardInteraction = remember { MutableInteractionSource() }
                                    val forwardPressed by forwardInteraction.collectIsPressedAsState()
                                    val forwardScale by animateFloatAsState(if (forwardPressed) 0.85f else 1f, label = "forwardScale")

                                    Box(
                                        modifier = Modifier
                                            .tvFocusRing(cornerRadius = 28.dp)
                                            .size(56.dp)
                                            .scale(forwardScale)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = if (forwardPressed) 0.8f else 0.5f))
                                            .onPreviewKeyEvent { handleSeekButtonKey(it.nativeKeyEvent, 1) }
                                            .clickable(
                                                interactionSource = forwardInteraction,
                                                indication = null
                                            ) {
                                                exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration))
                                                triggerControlsTimeout()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 80.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        formatTimeExo(if (isDraggingSlider) sliderPosition.toLong() else currentPos),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Slider(
                                        value = if (isDraggingSlider) sliderPosition else currentPos.toFloat(),
                                        onValueChange = {
                                            isDraggingSlider = true
                                            sliderPosition = it
                                            triggerControlsTimeout()
                                        },
                                        onValueChangeFinished = {
                                            isDraggingSlider = false
                                            val sp = sliderPosition.toLong().coerceIn(0L, duration)
                                            currentPos = sp
                                            exoPlayer.seekTo(sp)
                                            exoPlayer.playWhenReady = true
                                        },
                                        valueRange = 0f..(duration.coerceAtLeast(1L).toFloat()),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .padding(horizontal = 4.dp)
                                            .focusRequester(seekFocusRequester)
                                            .onFocusChanged { focusState ->
                                                seekBarHasFocus = focusState.isFocused
                                                if (!focusState.isFocused) finishDpadSeek()
                                            }
                                            .onPreviewKeyEvent { event ->
                                                val native = event.nativeKeyEvent
                                                if (!isTvDevice) {
                                                    false
                                                } else if (native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP &&
                                                    native.action == android.view.KeyEvent.ACTION_DOWN) {
                                                    finishDpadSeek()
                                                    runCatching { playFocusRequester.requestFocus() }
                                                    true
                                                } else if (native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
                                                    native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                                                    val direction = if (native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
                                                    when (native.action) {
                                                        android.view.KeyEvent.ACTION_DOWN -> {
                                                            beginDpadSeek(direction)
                                                            true
                                                        }
                                                        android.view.KeyEvent.ACTION_UP -> {
                                                            if (seekDirection == direction) finishDpadSeek()
                                                            true
                                                        }
                                                        else -> false
                                                    }
                                                } else {
                                                    false
                                                }
                                            },
                                        thumb = {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE50914))
                                                    .border(
                                                        width = if (isTvDevice && seekBarHasFocus) 3.dp else 0.dp,
                                                        color = if (isTvDevice && seekBarHasFocus) Color.White else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                            )
                                        },
                                        track = { ss ->
                                            SliderDefaults.Track(
                                                colors = SliderDefaults.colors(
                                                    activeTrackColor = Color(0xFFE50914),
                                                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                                                ),
                                                sliderState = ss,
                                                modifier = Modifier.height(7.dp),
                                                thumbTrackGapSize = 0.dp,
                                                drawStopIndicator = null
                                            )
                                        }
                                    )

                                    Text(
                                        formatTimeExo(duration),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .tvFocusRing(cornerRadius = 16.dp)
                                        .padding(8.dp)
                                        .focusRequester(episodesButtonFocusRequester)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            showEpisodesPanel = true
                                            triggerControlsTimeout()
                                        }
                                ) {
                                    Icon(
                                        Icons.Default.List,
                                        "Episodios",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text("Episodios", color = Color.White, fontSize = 11.sp)
                                }

                                var showAudioSubMenu by remember { mutableStateOf(false) }
                                Box {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .tvFocusRing(cornerRadius = 16.dp)
                                            .padding(8.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                showAudioSubMenu = true
                                                triggerControlsTimeout()
                                            }
                                    ) {
                                        Icon(
                                            Icons.Default.Subtitles,
                                            "Audio y Subtítulos",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text("Audio y Subtítulos", color = Color.White, fontSize = 11.sp)
                                    }

                                    DropdownMenu(
                                        expanded = showAudioSubMenu,
                                        onDismissRequest = { showAudioSubMenu = false },
                                        modifier = Modifier.background(Color(0xFF2B2B2B))
                                    ) {
                                        if (audioTracks.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Audio", color = Color.Gray, fontWeight = FontWeight.Bold) },
                                                onClick = { }
                                            )
                                            audioTracks.forEach { track ->
                                                val isSel = currentAudioStr == track.third
                                                DropdownMenuItem(
                                                    text = { Text(track.third, color = if (isSel) Color.White else Color.LightGray) },
                                                    onClick = {
                                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                                                            .setOverrideForType(TrackSelectionOverride(track.first, listOf(track.second)))
                                                            .build()
                                                        showAudioSubMenu = false
                                                    }
                                                )
                                            }
                                            HorizontalDivider(color = Color.DarkGray)
                                        }

                                        DropdownMenuItem(
                                            text = { Text("Subtítulos", color = Color.Gray, fontWeight = FontWeight.Bold) },
                                            onClick = { }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Apagados", color = if (currentSubStr == "Apagados") Color.White else Color.LightGray) },
                                            onClick = {
                                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                                    .build()
                                                currentSubStr = "Apagados"
                                                showAudioSubMenu = false
                                            }
                                        )
                                        subtitleTracks.forEach { track ->
                                            val isSel = currentSubStr == track.third
                                            DropdownMenuItem(
                                                text = { Text(track.third, color = if (isSel) Color.White else Color.LightGray) },
                                                onClick = {
                                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                        .setOverrideForType(TrackSelectionOverride(track.first, listOf(track.second)))
                                                        .build()
                                                    showAudioSubMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                if (onNextEpisode != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .tvFocusRing(cornerRadius = 16.dp)
                                            .padding(8.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                onNextEpisode?.invoke()
                                                triggerControlsTimeout()
                                            }
                                    ) {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            "Siguiente Episodio",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text("Siguiente Episodio", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, top = 24.dp, end = 16.dp, bottom = 16.dp)
                                    .align(Alignment.TopStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isFullscreen) {
                                    val backInteraction = remember { MutableInteractionSource() }
                                    val backPressed by backInteraction.collectIsPressedAsState()
                                    val backScale by animateFloatAsState(if (backPressed) 0.85f else 1f, label = "backScale")

                                    Box(
                                        modifier = Modifier
                                            .tvFocusRing(cornerRadius = 24.dp)
                                            .size(48.dp)
                                            .scale(backScale)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = if (backPressed) 0.8f else 0.4f))
                                            .clickable(
                                                interactionSource = backInteraction,
                                                indication = null
                                            ) {
                                                exitVideoPlayback()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Salir del video", tint = Color.White, modifier = Modifier.size(24.dp))
                                    }

                                    Spacer(Modifier.width(12.dp))
                                    Text(text = channelName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }

                                IconButton(onClick = {
                                    playerResizeMode = when (playerResizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                    triggerControlsTimeout()
                                }, modifier = Modifier.tvFocusRing(cornerRadius = 20.dp).size(36.dp)) {
                                    val icon = when (playerResizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> Icons.Default.AspectRatio
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> Icons.Default.ZoomIn
                                        else -> Icons.Default.Fullscreen
                                    }
                                    Icon(icon, "Relación de aspecto", tint = Color.White, modifier = Modifier.size(26.dp))
                                }

                                Spacer(Modifier.width(16.dp))

                                Box {
                                    IconButton(onClick = { showQualityMenu = true; triggerControlsTimeout() }, modifier = Modifier.tvFocusRing(cornerRadius = 20.dp).size(36.dp)) {
                                        Icon(Icons.Default.Hd, "Calidad", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                    DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }, modifier = Modifier.background(Color(0xFF2B2B2B))) {
                                        DropdownMenuItem(
                                            text = { Text("Auto", color = if (selectedQualityHeight == 0) Color(0xFFE50914) else Color.White, fontWeight = FontWeight.Bold) },
                                            onClick = { applyQuality(0); showQualityMenu = false }
                                        )
                                        availableQualities.forEach { quality ->
                                            val isSel = selectedQualityHeight == quality.second
                                            DropdownMenuItem(
                                                text = { Text(quality.first, color = if (isSel) Color(0xFFE50914) else Color.White) },
                                                onClick = { applyQuality(quality.second); showQualityMenu = false }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(16.dp))

                                Box(contentAlignment = Alignment.Center) {
                                    IconButton(onClick = { audioBoostLevel = (audioBoostLevel + 1) % 3; triggerControlsTimeout() }, modifier = Modifier.tvFocusRing(cornerRadius = 20.dp).size(36.dp)) {
                                        val boostColor = when (audioBoostLevel) { 1 -> Color(0xFFFFA500); 2 -> Color(0xFFFF0000); else -> Color.White }
                                        Icon(Icons.Default.VolumeUp, "Audio Boost", tint = boostColor, modifier = Modifier.size(26.dp))
                                    }
                                    if (audioBoostLevel > 0) {
                                        Text(text = if (audioBoostLevel == 1) "1.5x" else "2.0x", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).offset(y = 4.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                if (!isTvDevice) {
                                    IconButton(onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
                                            activity?.enterPictureInPictureMode(params)
                                        }
                                    }, modifier = Modifier.tvFocusRing(cornerRadius = 20.dp).size(36.dp)) {
                                        Icon(Icons.Default.PictureInPictureAlt, "PiP Mode", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                }
                            }

                            if (!isBuffering) {
                                Row(modifier = Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    val backInteraction = remember { MutableInteractionSource() }
                                    val backPressed by backInteraction.collectIsPressedAsState()
                                    val backScale by animateFloatAsState(if (backPressed) 0.85f else 1f, label = "backScale")

                                    Box(
                                        modifier = Modifier
                                            .tvFocusRing(cornerRadius = 24.dp)
                                            .size(48.dp)
                                            .scale(backScale)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = if (backPressed) 0.8f else 0.5f))
                                            .onPreviewKeyEvent { handleSeekButtonKey(it.nativeKeyEvent, -1) }
                                            .clickable(
                                                interactionSource = backInteraction,
                                                indication = null
                                            ) {
                                                exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                                                triggerControlsTimeout()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }

                                    Spacer(Modifier.width(if (isLandscape) 60.dp else 30.dp))

                                    val playInteraction = remember { MutableInteractionSource() }
                                    val playPressed by playInteraction.collectIsPressedAsState()
                                    val playScale by animateFloatAsState(if (playPressed) 0.85f else 1f, label = "playScale")

                                    Box(
                                        modifier = Modifier
                                            .focusRequester(playFocusRequester)
                                            .onFocusChanged { playHasFocus = it.isFocused }
                                            .tvFocusRing(cornerRadius = 36.dp)
                                            .size(72.dp)
                                            .scale(playScale)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = if (playPressed) 0.8f else 0.5f))
                                            .clickable(
                                                interactionSource = playInteraction,
                                                indication = null
                                            ) {
                                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                                triggerControlsTimeout()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(if (isLandscape) 60.dp else 30.dp))

                                    val forwardInteraction = remember { MutableInteractionSource() }
                                    val forwardPressed by forwardInteraction.collectIsPressedAsState()
                                    val forwardScale by animateFloatAsState(if (forwardPressed) 0.85f else 1f, label = "forwardScale")

                                    Box(
                                        modifier = Modifier
                                            .tvFocusRing(cornerRadius = 24.dp)
                                            .size(48.dp)
                                            .scale(forwardScale)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = if (forwardPressed) 0.8f else 0.5f))
                                            .onPreviewKeyEvent { handleSeekButtonKey(it.nativeKeyEvent, 1) }
                                            .clickable(
                                                interactionSource = forwardInteraction,
                                                indication = null
                                            ) {
                                                exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration))
                                                triggerControlsTimeout()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }

                            Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(formatTimeExo(if (isDraggingSlider) sliderPosition.toLong() else currentPos), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Slider(
                                    value = if (isDraggingSlider) sliderPosition else currentPos.toFloat(),
                                    onValueChange = { isDraggingSlider = true; sliderPosition = it; triggerControlsTimeout() },
                                    onValueChangeFinished = { isDraggingSlider = false; val sp = sliderPosition.toLong().coerceIn(0L, duration); currentPos = sp; exoPlayer.seekTo(sp); exoPlayer.playWhenReady = true },
                                    valueRange = 0f..(duration.coerceAtLeast(1L).toFloat()),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .padding(horizontal = 4.dp)
                                        .focusRequester(seekFocusRequester)
                                        .onFocusChanged { focusState ->
                                            seekBarHasFocus = focusState.isFocused
                                            if (!focusState.isFocused) finishDpadSeek()
                                        }
                                        .onPreviewKeyEvent { event ->
                                            val native = event.nativeKeyEvent
                                            if (!isTvDevice) {
                                                false
                                            } else if (native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP &&
                                                native.action == android.view.KeyEvent.ACTION_DOWN) {
                                                finishDpadSeek()
                                                runCatching { playFocusRequester.requestFocus() }
                                                true
                                            } else if (native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
                                                native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                                                val direction = if (native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
                                                when (native.action) {
                                                    android.view.KeyEvent.ACTION_DOWN -> {
                                                        beginDpadSeek(direction)
                                                        true
                                                    }
                                                    android.view.KeyEvent.ACTION_UP -> {
                                                        if (seekDirection == direction) finishDpadSeek()
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else {
                                                false
                                            }
                                        },
                                    thumb = {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE50914))
                                                .border(
                                                    width = if (isTvDevice && seekBarHasFocus) 3.dp else 0.dp,
                                                    color = if (isTvDevice && seekBarHasFocus) Color.White else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                        )
                                    },
                                    track = { ss ->
                                        SliderDefaults.Track(
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = Color(0xFFE50914),
                                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                                            ),
                                            sliderState = ss,
                                            modifier = Modifier.height(7.dp),
                                            thumbTrackGapSize = 0.dp,
                                            drawStopIndicator = null
                                        )
                                    }
                                )
                                Text(formatTimeExo(duration), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                                Spacer(Modifier.width(12.dp))

                                var showAudioSubMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showAudioSubMenu = true; triggerControlsTimeout() }, modifier = Modifier.tvFocusRing(cornerRadius = 20.dp).size(36.dp)) {
                                        Icon(Icons.Default.Settings, "Ajustes", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                    DropdownMenu(expanded = showAudioSubMenu, onDismissRequest = { showAudioSubMenu = false }, modifier = Modifier.background(Color(0xFF2B2B2B))) {
                                        if (audioTracks.isNotEmpty()) {
                                            DropdownMenuItem(text = { Text("Audio", color = Color.Gray, fontWeight = FontWeight.Bold) }, onClick = { })
                                            audioTracks.forEach { track ->
                                                val isSel = currentAudioStr == track.third
                                                DropdownMenuItem(text = { Text(track.third, color = if (isSel) Color.White else Color.LightGray) }, onClick = { exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(track.first, listOf(track.second))).build(); showAudioSubMenu = false })
                                            }
                                            HorizontalDivider(color = Color.DarkGray)
                                        }
                                        DropdownMenuItem(text = { Text("Subtítulos", color = Color.Gray, fontWeight = FontWeight.Bold) }, onClick = { })
                                        DropdownMenuItem(text = { Text("Apagados", color = if (currentSubStr == "Apagados") Color.White else Color.LightGray) }, onClick = { exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build(); currentSubStr = "Apagados"; showAudioSubMenu = false })
                                        subtitleTracks.forEach { track ->
                                            val isSel = currentSubStr == track.third
                                            DropdownMenuItem(text = { Text(track.third, color = if (isSel) Color.White else Color.LightGray) }, onClick = { exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).setOverrideForType(TrackSelectionOverride(track.first, listOf(track.second))).build(); showAudioSubMenu = false })
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                if (!PlayerUiState.isFullscreen && !isTvDevice) {
                                    IconButton(onClick = {
                                        PlayerUiState.isFullscreen = true
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        activity?.hideSystemBarsCompat()
                                    }, modifier = Modifier.tvFocusRing(cornerRadius = 20.dp).size(36.dp)) {
                                        Icon(Icons.Default.Fullscreen, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showEpisodesPanel && isSeries && isLandscape && !isPipMode) {

                val episodesListState = rememberLazyListState()
                LaunchedEffect(showEpisodesPanel, isTvDevice) {
                    if (showEpisodesPanel) {
                        val currentIndex = episodesList.indexOfFirst { it.url == currentEpisodeUrl }
                        if (currentIndex >= 0) {
                            episodesListState.scrollToItem(currentIndex)
                            if (isTvDevice) {
                                delay(200)
                                runCatching { currentEpisodeFocusRequester.requestFocus() }
                            }
                        }
                    }
                }


                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .zIndex(200f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Episodios",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { closeEpisodesPanel() },
                                modifier = Modifier
                                    .tvFocusRing(cornerRadius = 20.dp)
                                    .focusRequester(episodesCloseFocusRequester)
                                    .focusProperties {
                                        if (episodesList.isNotEmpty()) down = currentEpisodeFocusRequester
                                    }
                            ) {
                                Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        LazyRow(
                            state = episodesListState,
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 16.dp)
                        ) {
                            items(episodesList) { episode ->
                                val isCurrent = episode.url == currentEpisodeUrl

                                var epFocused by remember { mutableStateOf(false) }
                                Column(
                                    modifier = Modifier
                                        .width(200.dp)
                                        .then(if (isCurrent) Modifier.focusRequester(currentEpisodeFocusRequester) else Modifier)
                                        .focusProperties { up = episodesCloseFocusRequester }
                                        .scale(if (epFocused) 1.05f else 1f)
                                        .zIndex(if (epFocused) 1f else 0f)
                                        .onFocusChanged { epFocused = it.hasFocus }
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) Color(0xFFE50914).copy(alpha = 0.3f) else Color(0xFF1A1A1A))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onEpisodeSelected(episode.url)
                                            closeEpisodesPanel()
                                        }
                                        .border(
                                            width = if (epFocused) 3.dp else 0.dp,
                                            color = if (epFocused) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(112.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF333333)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (episode.logo.isNotBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(episode.logo)
                                                    .size(400)
                                                    .crossfade(false)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(40.dp)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "S${episode.seasonNum}:E${episode.episodeNum}",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .background(Color(0xFFE50914), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "VIENDO",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = "S${episode.seasonNum}:E${episode.episodeNum}",
                                        color = if (isCurrent) Color(0xFFE50914) else Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = episode.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (isCurrent) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Reproduciendo ahora",
                                            color = Color(0xFFE50914),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTimeExo(timeMs: Long): String {
    if (timeMs < 0) return "00:00"
    val ts = timeMs / 1000
    val h = ts / 3600
    val m = (ts % 3600) / 60
    val s = ts % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
