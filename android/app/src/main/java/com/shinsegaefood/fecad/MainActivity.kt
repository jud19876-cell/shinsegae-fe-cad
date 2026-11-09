package com.shinsegaefood.fecad

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var autoUpdater: AutoUpdater
    private var checkTimer: Timer? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        const val LIVE_HTML_URL = "https://raw.githubusercontent.com/jud19876-cell/shinsegae-fe-cad/main/shinsegae_fe_sketchup_studio.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoUpdater = AutoUpdater(this)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(WebAppBridge(autoUpdater), "AndroidAutoUpdater")

        // 1. 기존에 받아둔 최신 Live HTML이 있으면 우선 로드, 없으면 APK 내장 번들 로드
        val liveFile = File(filesDir, "live_studio.html")
        if (liveFile.exists() && liveFile.length() > 1000) {
            webView.loadUrl("file://${liveFile.absolutePath}")
        } else {
            webView.loadUrl("file:///android_asset/index.html")
        }

        // 2. 무설치 Live Web OTA 동기화 실행 (앱 켤 때 즉시 백그라운드 확인)
        syncLiveWebPatch()

        // 3. 네이티브 APK 업데이트 체크 (15분 주기)
        startPeriodicUpdateCheck()
    }

    private fun syncLiveWebPatch() {
        executor.execute {
            try {
                val url = URL(LIVE_HTML_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 6000
                conn.setRequestProperty("User-Agent", "ShinsegaeFE-Android")
                conn.useCaches = false
                if (conn.responseCode == 200) {
                    val newHtml = conn.inputStream.bufferedReader().use { it.readText() }
                    val liveFile = File(filesDir, "live_studio.html")
                    val isFirstDownload = !liveFile.exists()
                    val isUpdated = isFirstDownload || (liveFile.readText() != newHtml)

                    if (isUpdated && newHtml.contains("SHINSEGAE FOOD FE")) {
                        liveFile.writeText(newHtml)
                        mainHandler.post {
                            Toast.makeText(this, "✨ 최신 도면·설비 패치가 자동 적용되었습니다", Toast.LENGTH_SHORT).show()
                            webView.loadUrl("file://${liveFile.absolutePath}")
                        }
                    }
                }
            } catch (_: Exception) {
                // 오프라인 상태: 이전에 받아둔 로컬 파일로 정상 동작
            }
        }
    }

    private fun startPeriodicUpdateCheck() {
        checkTimer?.cancel()
        checkTimer = Timer()
        checkTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                syncLiveWebPatch()
                autoUpdater.checkAndPrefetchLatestRelease()
            }
        }, 15 * 60 * 1000L, 15 * 60 * 1000L) // 15분 주기
    }

    override fun onStop() {
        super.onStop()
        autoUpdater.installPendingUpdateOnExit()
    }

    override fun onDestroy() {
        checkTimer?.cancel()
        super.onDestroy()
    }

    class WebAppBridge(private val updater: AutoUpdater) {
        @JavascriptInterface
        fun prefetchUpdate(apkUrl: String, tag: String) {
            updater.downloadApkInBackground(apkUrl, tag)
        }

        @JavascriptInterface
        fun installPrefetchedOrDownload(apkUrl: String) {
            updater.installNow(apkUrl)
        }
    }
}
