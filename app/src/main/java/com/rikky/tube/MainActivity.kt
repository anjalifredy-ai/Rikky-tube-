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

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomNav: BottomNavigationView

    private val homeUrl = "https://www.youtube.com/"
    private val shortsUrl = "https://www.youtube.com/shorts"
    private val subscriptionsUrl = "https://www.youtube.com/feed/subscriptions"
    private val musicUrl = "https://music.youtube.com/"

    private val cleanupJs = """
        (function() {
            function hideStuff() {
                var css = `
                    ytm-mobile-topbar-renderer,
                    ytm-pivot-bar-renderer,
                    tp-yt-app-drawer,
                    ytm-app-bar-renderer,
                    #masthead,
                    ytd-masthead,
                    #mobile-topbar-renderer,
                    .pivotBar,
                    ytm-companion-ad-renderer,
                    ytm-promoted-sparkles-web-renderer,
                    ytm-banner-promo-renderer,
                    ytm-statement-banner-renderer,
                    ytm-in-feed-ad-layout-renderer,
                    ytm-ad-slot-renderer,
                    .ytp-ad-module,
                    .video-ads,
                    #player-ads,
                    ytd-display-ad-renderer,
                    ytd-promoted-video-renderer,
                    ytd-ad-slot-renderer {
                        display: none !important;
                        height: 0 !important;
                    }
                `;
                var style = document.getElementById('rikky-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'rikky-style';
                    document.head.appendChild(style);
                }
                style.innerHTML = css;

                var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern');
                if (skipBtn) { skipBtn.click(); }
            }
            hideStuff();
            setInterval(hideStuff, 1500);
        })();
    """.trimIndent()

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

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.userAgentString = webView.settings.userAgentString +
            " RikkYTubeApp/1.0"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
                view?.evaluateJavascript(cleanupJs, null)
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
                if (newProgress > 50) {
                    view?.evaluateJavascript(cleanupJs, null)
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
