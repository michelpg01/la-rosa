

==============================
ARCHIVO: build.gradle.kts
RUTA: C:/Users/Maicol/AndroidStudioProjects/LatinoIPTVpro\app\build.gradle.kts
==============================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

android {

    namespace = "com.example.latinoiptvpro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.latinoiptvpro"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.5"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("D:/key_app/latinoiptvpro.keystore")
            storePassword = "aithanna02"
            keyAlias = "latinoiptvpro"
            keyPassword = "aithanna02"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.generateKotlin", "true")
    }
}

dependencies {

    // =========================================================
    // CORE & UI
    // =========================================================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("androidx.media3:media3-exoplayer-dash:1.5.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // =========================================================
    // NAVIGATION
    // =========================================================
    implementation(libs.androidx.navigation.compose)

    // =========================================================
    // LIFECYCLE
    // =========================================================
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // =========================================================
    // VLC + MEDIA3 (ExoPlayer)
    // =========================================================
    implementation(libs.libvlc.all)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation("androidx.media3:media3-datasource-okhttp:1.5.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.3.1+2")

    // =========================================================
    // IMAGES
    // =========================================================
    implementation(libs.coil.compose)

    // =========================================================
    // DATASTORE
    // =========================================================
    implementation(libs.datastore.preferences)

    // =========================================================
    // SERIALIZATION
    // =========================================================
    implementation(libs.kotlinx.serialization.json)

    // =========================================================
    // FIREBASE
    // =========================================================
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.analytics.ktx)

    // =========================================================
    // ROOM DATABASE
    // =========================================================
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // =========================================================
    // PAGING 3
    // =========================================================
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // =========================================================
    // JSON
    // =========================================================
    implementation(libs.gson)
    implementation(libs.json)

    // =========================================================
    // PROFILEINSTALLER (para Baseline Profiles manual)
    // =========================================================
    implementation(libs.androidx.profileinstaller)
}

==============================
ARCHIVO: UpdateManager.kt
RUTA: C:/Users/Maicol/AndroidStudioProjects/LatinoIPTVpro\app\src\main\java\com\example\latinoiptvpro\update\UpdateManager.kt
==============================

package com.example.latinoiptvpro.update

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val UPDATE_JSON_URL =
        "https://www.dropbox.com/scl/fi/ss8vbj7qgohp6gq10fhp1/update_info.json?rlkey=sn2i3pselhl70e7tt3yhbn3bz&st=f26h37kz&dl=1"

    private const val PREFS_NAME = "update_prefs"
    private const val KEY_SKIP_UNTIL = "skip_until"

    suspend fun checkForUpdate(
        context: Context,
        onUpdateAvailable: (UpdateInfo) -> Unit,
        onNoUpdate: () -> Unit = {}
    ) {
        try {
            val jsonString = withContext(Dispatchers.IO) {
                URL(UPDATE_JSON_URL).readText()
            }
            val json = JSONObject(jsonString)

            val latestVersionCode = json.getInt("versionCode")
            val latestVersionName = json.getString("versionName")
            val downloadUrl = json.getString("downloadUrl")
            val changelog = json.optString("changelog", "Mejoras y correcciones.")

            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionCode = packageInfo.versionCode
            val currentVersionName = packageInfo.versionName ?: ""

            if (latestVersionCode > currentVersionCode) {
                val updateInfo = UpdateInfo(
                    versionCode = latestVersionCode,
                    versionName = latestVersionName,
                    downloadUrl = downloadUrl,
                    changelog = changelog,
                    currentVersionName = currentVersionName
                )
                onUpdateAvailable(updateInfo)
            } else {
                onNoUpdate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onNoUpdate()
        }
    }

    fun downloadApk(
        activity: Activity,
        updateInfo: UpdateInfo,
        onProgress: (Int) -> Unit,
        onComplete: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(updateInfo.downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                connection.connect()

                val fileLength = connection.contentLength
                val inputStream = connection.inputStream

                val apkFile = File(
                    activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "actualizacion_${updateInfo.versionName}.apk"
                )
                if (apkFile.exists()) apkFile.delete()

                val outputStream = FileOutputStream(apkFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                var lastProgress = -1

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (fileLength > 0) {
                        val progress = (totalRead * 100 / fileLength).toInt()
                        if (progress != lastProgress) {
                            lastProgress = progress
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                }

                outputStream.close()
                inputStream.close()
                connection.disconnect()

                withContext(Dispatchers.Main) { onComplete(apkFile) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onError("Error de descarga: ${e.message}") }
            }
        }
    }

    fun downloadAndInstall(context: Context, updateInfo: UpdateInfo) {
        try {
            val apkFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "actualizacion_${updateInfo.versionName}.apk"
            )
            if (apkFile.exists()) apkFile.delete()

            val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
                .setTitle("Descargando actualización...")
                .setDescription("Latino IPTV Pro v${updateInfo.versionName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(apkFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                request.setRequiresDeviceIdle(false)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        installApk(context, apkFile)
                        context.unregisterReceiver(this)
                    }
                }
            }

            context.registerReceiver(
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
            Toast.makeText(context, "Descargando actualización...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al iniciar la descarga", Toast.LENGTH_SHORT).show()
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(context, "Permite instalar apps de fuentes desconocidas", Toast.LENGTH_LONG).show()
                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}"))
                    context.startActivity(settingsIntent)
                    return
                }
            }

            context.startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al instalar la actualización", Toast.LENGTH_SHORT).show()
        }
    }

    fun skipUpdate(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val skipUntil = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L
        prefs.edit().putLong(KEY_SKIP_UNTIL, skipUntil).apply()
    }

    fun isSkipped(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val skipUntil = prefs.getLong(KEY_SKIP_UNTIL, 0L)
        return System.currentTimeMillis() < skipUntil
    }
}

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String,
    val currentVersionName: String
)
