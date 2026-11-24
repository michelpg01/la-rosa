package com.example.latinoiptvpro

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Process
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.latinoiptvpro.data.local.SessionManager
import com.example.latinoiptvpro.navigation.AppNavigation
import com.example.latinoiptvpro.ui.components.PlayerUiState
import com.example.latinoiptvpro.ui.components.globalIsPipMode
import com.example.latinoiptvpro.ui.m3u.M3uWatchHistoryStore
import com.example.latinoiptvpro.update.UpdateManager
import com.example.latinoiptvpro.update.UpdateInfo
import com.example.latinoiptvpro.utils.AdBlockManager
import com.example.latinoiptvpro.utils.SecurityProvider
import com.example.latinoiptvpro.utils.isTv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val EXPECTED_SIGNATURE = "48BEC634D99E23A54DA9FBA7B7C4F034B7C15E8BE8E53798CB436CDF25B52F9F"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (isDebuggerAttached()) {
            killProcess()
            return
        }

        if (!isSignatureValid()) {
            killProcess()
            return
        }

        SecurityProvider.validatePins()

        AdBlockManager.initialize(this)

        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ✅ Solo forzar portrait en celular/tablet. En TV NO.
        if (!isTv()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val sessionManager = SessionManager(this)
        val startRoute = if (sessionManager.isValidSession()) "splash" else "login"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader.Builder(this@MainActivity)
                    .memoryCache {
                        MemoryCache.Builder(this@MainActivity)
                            .maxSizePercent(0.10)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(cacheDir.resolve("image_cache"))
                            .maxSizeBytes(50L * 1024 * 1024)
                            .build()
                    }
                    .crossfade(true)
                    .build()
                Coil.setImageLoader(imageLoader)
            } catch (e: Exception) {
                Log.e(TAG, "Error al inicializar Coil", e)
            }
        }

        M3uWatchHistoryStore.init(this)

        setContent {
            LatinoIptvProTheme {
                val isFullscreen = PlayerUiState.isFullscreen
                val isPipMode = globalIsPipMode

                SideEffect {
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    if (isFullscreen && !isPipMode) {
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                    } else if (!isPipMode) {
                        controller.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                var showForceUpdateDialog by remember { mutableStateOf(false) }
                var isDownloading by remember { mutableStateOf(false) }
                var downloadProgress by remember { mutableStateOf(0) }
                var downloadError by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    delay(1500)
                    UpdateManager.checkForUpdate(
                        context = this@MainActivity,
                        onUpdateAvailable = { info ->
                            updateInfo = info
                            showForceUpdateDialog = true
                        },
                        onNoUpdate = { }
                    )
                }

                if (showForceUpdateDialog && updateInfo != null) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text("Actualización Obligatoria 🚀") },
                        text = {
                            Column {
                                Text("Versión actual: ${updateInfo!!.currentVersionName}")
                                Text("Nueva versión: ${updateInfo!!.versionName}")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Novedades:")
                                Text(updateInfo!!.changelog)
                                Spacer(modifier = Modifier.height(16.dp))

                                if (isDownloading) {
                                    LinearProgressIndicator(
                                        progress = downloadProgress / 100f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Descargando: $downloadProgress%")
                                } else if (downloadError != null) {
                                    Text("Error: $downloadError", color = Color.Red)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = {
                                        downloadError = null
                                        isDownloading = true
                                        downloadProgress = 0
                                        UpdateManager.downloadApk(
                                            activity = this@MainActivity,
                                            updateInfo = updateInfo!!,
                                            onProgress = { progress -> downloadProgress = progress },
                                            onComplete = { file ->
                                                isDownloading = false
                                                showForceUpdateDialog = false
                                                UpdateManager.installApk(this@MainActivity, file)
                                            },
                                            onError = { error ->
                                                isDownloading = false
                                                downloadError = error
                                            }
                                        )
                                    }) {
                                        Text("Reintentar")
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            if (!isDownloading && downloadError == null) {
                                Button(onClick = {
                                    isDownloading = true
                                    downloadProgress = 0
                                    UpdateManager.downloadApk(
                                        activity = this@MainActivity,
                                        updateInfo = updateInfo!!,
                                        onProgress = { progress -> downloadProgress = progress },
                                        onComplete = { file ->
                                            isDownloading = false
                                            showForceUpdateDialog = false
                                            UpdateManager.installApk(this@MainActivity, file)
                                        },
                                        onError = { error ->
                                            isDownloading = false
                                            downloadError = error
                                        }
                                    )
                                }) {
                                    Text("Actualizar ahora")
                                }
                            }
                        },
                        dismissButton = null
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(startDestination = startRoute)
                }
            }
        }
    }

    private fun isDebuggerAttached(): Boolean {
        if (Debug.isDebuggerConnected()) return true
        try {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(java.io.FileInputStream("/proc/self/status")))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.startsWith("TracerPid:")) {
                    val tracerPid = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                    reader.close()
                    return tracerPid > 0
                }
            }
            reader.close()
        } catch (_: Exception) { }
        return false
    }

    private fun isReleaseBuild(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0
    }

    private fun isSignatureValid(): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return false

            val md = MessageDigest.getInstance("SHA-256")
            val signatureHash = md.digest(signatures[0].toByteArray())
            val hexString = signatureHash.joinToString("") { "%02X".format(it) }

            if (!isReleaseBuild()) return true
            hexString == EXPECTED_SIGNATURE
        } catch (e: Exception) {
            !isReleaseBuild()
        }
    }

    private fun killProcess() {
        try { Thread.sleep(200) } catch (_: InterruptedException) { }
        finishAndRemoveTask()
        Process.killProcess(Process.myPid())
        System.exit(0)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        globalIsPipMode = isInPictureInPictureMode
    }
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFF36D760),
    background = Color(0xFF101828),
    surface = Color(0xFF151E30),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF36D760),
    background = Color(0xFF101828),
    surface = Color(0xFF151E30),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun LatinoIptvProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val currentDensity = LocalDensity.current
    val minWidthDp = 410f
    val screenWidth = configuration.screenWidthDp.toFloat()
    val scaleFactor = if (screenWidth < minWidthDp) screenWidth / minWidthDp else 1f
    val customDensity = remember(currentDensity, configuration.screenWidthDp) {
        Density(
            density = currentDensity.density * scaleFactor,
            fontScale = 1.0f
        )
    }
    CompositionLocalProvider(LocalDensity provides customDensity) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors
        ) {
            content()
        }
    }
}
