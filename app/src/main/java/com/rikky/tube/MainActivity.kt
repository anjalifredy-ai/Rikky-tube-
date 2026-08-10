package com.rikky.tube

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.widget.ProgressBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.Base64
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var searchButton: ImageButton

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val homeUrl = "https://www.youtube.com/"
    private val shortsUrl = "https://www.youtube.com/shorts"
    private val subscriptionsUrl = "https://www.youtube.com/feed/subscriptions"
    private val musicUrl = "https://music.youtube.com/"

    private val adDomains = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "static.doubleclick.net",
        "/api/stats/ads",
        "/pagead",
        "/ptracking",
        "/get_midroll",
        "/api/stats/qoe",
        "googletagmanager.com",
        "googletagservices.com",
        "amazon-adsystem.com",
        "adnxs.com",
        "moatads.com",
        "adsafeprotected.com",
        "imasdk.googleapis.com",
        "youtubei/v1/player/ad_break",
        "/annotations_invideo",
        "/api/stats/atr"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        bottomNav = findViewById(R.id.bottomNav)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)
        searchButton = findViewById(R.id.searchButton)

        setupWebView()
        setupBottomNav()
        setupSearchButton()

        if (savedInstanceState == null) {
            webView.loadUrl(homeUrl)
        }
    }

    private fun setupSearchButton() {
        searchButton.setOnClickListener {
            webView.loadUrl("https://www.youtube.com/results")
        }
    }

    private fun readCriticalCss(): String {
        return try {
            val inputStream = assets.open("critical.css")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val text = reader.readText()
            reader.close()
            text
        } catch (e: Exception) {
            ""
        }
    }

    private fun isAdRequest(url: String): Boolean {
        return adDomains.any { url.contains(it, ignoreCase = true) }
    }

    private fun blockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream("".toByteArray())
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.userAgentString = webView.settings.userAgentString +
            " RikkYTubeApp/1.0"

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            val css = readCriticalCss()
            if (css.isNotEmpty()) {
                val encoded = Base64.getEncoder().encodeToString(css.toByteArray(Charsets.UTF_8))
                val js = """
                    (function(){
                        var style = document.createElement('style');
                        style.textContent = window.atob('$encoded');
                        (document.head || document.documentElement).appendChild(style);
                    })();
                """.trimIndent()
                WebViewCompat.addDocumentStartJavaScript(webView, js, Collections.singleton("*"))
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                if (isAdRequest(url)) {
                    return blockedResponse()
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) {
                    ProgressBar.VISIBLE
                } else {
                    ProgressBar.GONE
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback

                fullscreenContainer.addView(
                    view,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE

                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }

            override fun onHideCustomView() {
                fullscreenContainer.removeAllViews()
                fullscreenContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null

                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }

        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    webView.loadUrl(homeUrl)
                    true
                }
                R.id.nav_shorts -> {
                    webView.loadUrl(shortsUrl)
                    true
                }
                R.id.nav_subscriptions -> {
                    webView.loadUrl(subscriptionsUrl)
                    true
                }
                R.id.nav_music -> {
                    webView.loadUrl(musicUrl)
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if (customView != null) {
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
