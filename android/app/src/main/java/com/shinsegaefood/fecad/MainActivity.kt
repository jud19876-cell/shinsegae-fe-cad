package com.shinsegaefood.fecad

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var autoUpdater: AutoUpdater
    private var checkTimer: Timer? = null

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

        webView.loadUrl("file:///android_asset/index.html")

        // 앱을 열 때마다 + 15분마다 GitHub 최신 릴리스 확인
        startPeriodicUpdateCheck()
    }

    private fun startPeriodicUpdateCheck() {
        checkTimer?.cancel()
        checkTimer = Timer()
        checkTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                autoUpdater.checkAndPrefetchLatestRelease()
            }
        }, 0L, 15 * 60 * 1000L) // 즉시 1회 + 15분 주기
    }

    override fun onStop() {
        super.onStop()
        // 화면을 보고 있는 동안에는 설치하지 않고, 앱에서 나가는 순간 미리 받아둔 APK를 PackageInstaller로 설치
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
