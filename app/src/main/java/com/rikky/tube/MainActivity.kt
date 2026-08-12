package com.rikky.tube

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.Base64
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var bottomNav: BottomNavigationView? = null
    private var fullscreenContainer: FrameLayout? = null
    private var searchButton: ImageButton? = null
    private var searchBox: EditText? = null
    private var topBar: TextView? = null

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val homeUrl = "https://www.youtube.com/"
    private val shortsUrl = "https://www.youtube.com/shorts"
    private val subscriptionsUrl = "https://www.youtube.com/feed/subscriptions"
    private val youUrl = "https://www.youtube.com/feed/you"
    private val musicUrl = "https://music.youtube.com/"

    private val adDomains = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "adservice.google.com", "pagead2.googlesyndication.com",
        "static.doubleclick.net", "googletagmanager.com", "googletagservices.com",
        "amazon-adsystem.com", "adnxs.com", "moatads.com", "adsafeprotected.com",
        "imasdk.googleapis.com", "2mdn.net", "adform.net", "adroll.com",
        "criteo.com", "criteo.net", "outbrain.com", "taboola.com",
        "scorecardresearch.com", "quantserve.com", "adsystem.com", "advertising.com",
        "serving-sys.com", "yieldmanager.com", "casalemedia.com", "rubiconproject.com",
        "openx.net", "pubmatic.com", "smartadserver.com", "adtechus.com",
        "media.net", "innovid.com", "flashtalking.com", "sitescout.com",
        "chartboost.com", "unityads.unity3d.com", "vungle.com", "applovin.com",
        "startapp.com", "airpush.com", "ads.youtube.com",
        "youtube.com/api/stats/ads", "youtube.com/pagead", "youtube.com/ptracking",
        "youtube.com/get_midroll", "youtube.com/api/stats/qoe", "youtube.com/api/stats/atr",
        "youtubei/v1/player/ad_break", "youtubei/v1/log_event",
        "youtubei/v1/att/get", "/player_204", "/ad_break",
        "/annotations_invideo", "/generate_204", "/pagead/", "/ptracking",
        "/api/stats/ads", "/api/stats/watchtime", "/csi_204",
        "doubleclick", "googleads", "adserver", "adservice"
    )

    private val playerAdBlockJs = "(function(){function skipAdsAndHide(){try{var skipBtn=document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');if(skipBtn){skipBtn.click();}var adEls=document.querySelectorAll('.ytp-ad-overlay-container, .ytp-ad-text-overlay, ytd-promoted-sparkles-web-renderer, ytm-promoted-sparkles-web-renderer, ytd-display-ad-renderer, ytd-promoted-video-renderer, ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer, ytm-companion-ad-renderer');for(var i=0;i<adEls.length;i++){adEls[i].style.display='none';}}catch(e){}}skipAdsAndHide();setInterval(skipAdsAndHide,800);})();"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            webView = findViewById(R.id.webView)
            progressBar = findViewById(R.id.progressBar)
            bottomNav = findViewById(R.id.bottomNav)
            fullscreenContainer = findViewById(R.id.fullscreenContainer)
            searchButton = findViewById(R.id.searchButton)
            searchBox = findViewById(R.id.searchBox)
            topBar = findViewById(R.id.topBar)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setupWebView()
        setupBottomNav()
        setupSearch()

        if (savedInstanceState == null) {
            webView?.loadUrl(homeUrl)
        }
    }

    private fun performSearch() {
        val query = searchBox?.text?.toString()?.trim() ?: ""
        if (query.isNotEmpty()) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            webView?.loadUrl("https://www.youtube.com/results?search_query=" + encoded)
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(searchBox?.windowToken, 0)
            searchBox?.clearFocus()
        }
    }

    private fun setupSearch() {
        searchButton?.setOnClickListener {
            performSearch()
        }
        searchBox?.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performSearch()
                true
            } else {
                false
            }
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
        val wv = webView ?: return

        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.loadWithOverviewMode = true
        wv.settings.useWideViewPort = true
        wv.settings.mediaPlaybackRequiresUserGesture = false
        wv.settings.userAgentString = wv.settings.userAgentString + " RikkYTubeApp/1.0"

        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                val css = readCriticalCss()
                if (css.isNotEmpty()) {
                    val encoded = Base64.getEncoder().encodeToString(css.toByteArray(Charsets.UTF_8))
                    val js = "(function(){var style=document.createElement('style');style.textContent=window.atob('" + encoded + "');(document.head||document.documentElement).appendChild(style);})();"
                    WebViewCompat.addDocumentStartJavaScript(wv, js, Collections.singleton("*"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        wv.webViewClient = object : WebViewClient() {
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
                webView?.evaluateJavascript(playerAdBlockJs, null)
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar?.progress = newProgress
                progressBar?.visibility = if (newProgress in 1..99) {
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

                fullscreenContainer?.addView(
                    view,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                fullscreenContainer?.visibility = View.VISIBLE
                webView?.visibility = View.GONE
                topBar?.visibility = View.GONE
                searchBox?.visibility = View.GONE
                searchButton?.visibility = View.GONE
                bottomNav?.visibility = View.GONE
                progressBar?.visibility = View.GONE

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
                fullscreenContainer?.removeAllViews()
                fullscreenContainer?.visibility = View.GONE
                webView?.visibility = View.VISIBLE
                topBar?.visibility = View.VISIBLE
                searchBox?.visibility = View.VISIBLE
                searchButton?.visibility = View.VISIBLE
                bottomNav?.visibility = View.VISIBLE
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null

                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    private fun setupBottomNav() {
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { webView?.loadUrl(homeUrl); true }
                R.id.nav_shorts -> { webView?.loadUrl(shortsUrl); true }
                R.id.nav_subscriptions -> { webView?.loadUrl(subscriptionsUrl); true }
                R.id.nav_you -> { webView?.loadUrl(youUrl); true }
                R.id.nav_music -> { webView?.loadUrl(musicUrl); true }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if (customView != null) {
            webView?.webChromeClient?.onHideCustomView()
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
