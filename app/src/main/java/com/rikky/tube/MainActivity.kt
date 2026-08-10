package com.rikky.tube

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.widget.ProgressBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Base64
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomNav: BottomNavigationView

    private val homeUrl = "https://www.youtube.com/"
    private val shortsUrl = "https://www.youtube.com/shorts"
    private val subscriptionsUrl = "https://www.youtube.com/feed/subscriptions"
    private val musicUrl = "https://music.youtube.com/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        bottomNav = findViewById(R.id.bottomNav)

        setupWebView()
        setupBottomNav()

        if (savedInstanceState == null) {
            webView.loadUrl(homeUrl)
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.userAgentString = webView.settings.userAgentString +
            " RikkYTubeApp/1.0"

        // CSS ko page load hone se PEHLE hi inject karo (document-start)
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
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
