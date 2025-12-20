package com.xiaprojects.rb

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.http.SslError
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.SslErrorHandler
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.xiaprojects.rb.MainActivity.BundleExtraParamsConst


open class FullScreenWebViewActivity : AppCompatActivity() {
    protected lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide ActionBar
        supportActionBar?.hide()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }



        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)

        webView.settings.apply {
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true
            domStorageEnabled = true

            // Enable caching
            //cacheMode = WebSettings.LOAD_DEFAULT
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

            // Optional: improve performance
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            loadsImagesAutomatically = true
            setSupportZoom(true)

            // Optional: enable local file access if needed
//            allowFileAccess = true
//            allowContentAccess = true
        }

        /*
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        */
        webView.webViewClient = object : WebViewClient() {

//            override fun onReceivedSslError(
//                view: WebView,
//                handler: SslErrorHandler,
//                error: SslError
//            ) {
//                // ignore all ssl errors
//                handler.proceed()
//            }
        }
    }

    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }
}

class MainActivity : FullScreenWebViewActivity() {

    object BundleExtraParamsConst {
        const val APP_URL = "app_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //
        super.onCreate(savedInstanceState)

        val appUrl = intent.getStringExtra(BundleExtraParamsConst.APP_URL) ?: resources.getString(R.string.defaultUrl)

        //
        webView.loadUrl(appUrl)

        //
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays
        for (display in displays) {
            if (display.displayId != Display.DEFAULT_DISPLAY) {
                val options = ActivityOptions.makeBasic()
                options.launchDisplayId = display.displayId
                startActivity(
                    Intent(this@MainActivity, SecondActivity::class.java).apply {
                        putExtra(BundleExtraParamsConst.APP_URL, appUrl)
                    },
                    options.toBundle()
                )
            }
        }
    }
}


class SecondActivity : FullScreenWebViewActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        //
        super.onCreate(savedInstanceState)

        val appUrl = intent.getStringExtra(BundleExtraParamsConst.APP_URL) ?: resources.getString(R.string.defaultUrl)

        webView.loadUrl(appUrl)
    }
}

