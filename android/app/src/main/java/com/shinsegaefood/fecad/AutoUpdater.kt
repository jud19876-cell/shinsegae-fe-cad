package com.shinsegaefood.fecad

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class AutoUpdater(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var pendingApkFile: File? = null
    @Volatile private var pendingVersionCode: String? = null

    companion object {
        // GitHub 저장소 경로
        const val GITHUB_REPO = "jud19876-cell/shinsegae-fe-cad"
    }

    fun checkAndPrefetchLatestRelease() {
        executor.execute {
            try {
                val apiUrl = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                val conn = apiUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                if (conn.responseCode != 200) return@execute

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val latestTag = json.optString("tag_name", "")
                val currentVer = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""

                if (latestTag.isNotEmpty() && latestTag != currentVer && latestTag != pendingVersionCode) {
                    val assets = json.optJSONArray("assets") ?: return@execute
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            val downloadUrl = asset.optString("browser_download_url", "")
                            downloadApkInBackground(downloadUrl, latestTag)
                            break
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun downloadApkInBackground(apkUrl: String, tag: String) {
        executor.execute {
            try {
                val targetFile = File(context.cacheDir, "update_${tag}.apk")
                if (!targetFile.exists()) {
                    URL(apkUrl).openStream().use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                pendingApkFile = targetFile
                pendingVersionCode = tag
            } catch (_: Exception) {}
        }
    }

    fun installPendingUpdateOnExit() {
        val apk = pendingApkFile ?: return
        if (!apk.exists()) return
        pendingApkFile = null
        installWithPackageInstaller(apk)
    }

    fun installNow(apkUrl: String) {
        val apk = pendingApkFile
        if (apk != null && apk.exists()) {
            pendingApkFile = null
            installWithPackageInstaller(apk)
        } else {
            executor.execute {
                downloadApkInBackground(apkUrl, "immediate")
                pendingApkFile?.let { installWithPackageInstaller(it) }
            }
        }
    }

    private fun installWithPackageInstaller(apkFile: File) {
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // 안드로이드 12+ 첫 1회 설치 이후 확인창 없이 자동 덮어쓰기 업데이트
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            apkFile.inputStream().use { input ->
                session.openWrite("fe_cad_update.apk", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val intent = Intent(context, MainActivity::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, sessionId, intent, flags)
            session.commit(pendingIntent.intentSender)
            session.close()
        } catch (_: Exception) {}
    }
}
