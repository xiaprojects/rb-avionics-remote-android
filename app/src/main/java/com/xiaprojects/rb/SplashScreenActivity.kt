package com.xiaprojects.rb

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import androidx.core.net.toUri
import java.net.URI
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import androidx.preference.PreferenceManager
import org.json.JSONArray


class SplashScreenActivity : ComponentActivity() {

    object SettingsConst {
        const val APP_URL = "siteUrl"
        const val BACK_BUTTON_MODE = "backButton"
        const val MODE_HISTORY_AND_EXIT = "history_and_exit"
        const val MODE_HISTORY = "history_only"
        const val MODE_EXIT = "exit_only"
        const val MODE_NOTHING = "nothing"
        const val SETTINGS_BUTTON_TIMEOUT = "settingsTimeout"
        const val LAST_ACCEPTED_SSL_PROBLEM_USL = "last_accepted_ssl_error_url"
        const val API_TIMEOUT = "apiTimeout"
        const val KEEP_SCREEN_ON = "keepScreenOn"
    }

    object ExtraBundleConst {
        const val FORCE_URL_DIALOG = "force_url_dialog"
        const val OPEN_SETTINGS = "openSettings"
    }

    private lateinit var loadingImage: ImageView
    private lateinit var errorText: TextView
    private lateinit var urlEditorLayout: View
    private lateinit var urlEditText: EditText
    private lateinit var retryButton: Button
    private lateinit var saveButton: Button
    private lateinit var rotateAnim: Animation

    private lateinit var splashscreen: SplashScreen

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        //val splashScreen = SplashScreen.
        splashscreen = installSplashScreen().apply {
            setKeepOnScreenCondition {
                // Keeps the splash screen visible while loading (optional logic can be applied here)
                true
            }
        }
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        // UI references
        loadingImage = findViewById(R.id.loading_image)
        errorText = findViewById(R.id.error_text)
        urlEditorLayout = findViewById(R.id.url_editor_layout)
        urlEditText = findViewById(R.id.url_edit_text)
        retryButton = findViewById(R.id.retry_button)
        saveButton = findViewById(R.id.save_button)

        val appUrl = resources.getString(R.string.defaultUrl)

        // Load saved URL or default
        val savedUrl = prefs.getString(SettingsConst.APP_URL, appUrl)!!
        urlEditText.setText(savedUrl)

        // Load animation
        rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate)

        splashscreen.apply {
            setKeepOnScreenCondition {
                // Keeps the splash screen visible while loading (optional logic can be applied here)
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val openSett = intent.getBooleanExtra(ExtraBundleConst.OPEN_SETTINGS, false)

        val savedUrl = prefs.getString(SettingsConst.APP_URL, "")!!

        if (savedUrl == "" || openSett) {
            intent.removeExtra(ExtraBundleConst.OPEN_SETTINGS)
            startActivity(Intent(this, SettingsActivity::class.java))
        } else {
            startConnectionCheck(savedUrl)
        }
    }

    private fun startConnectionCheck(url: String) {
        loadingImage.visibility = View.VISIBLE
        loadingImage.startAnimation(rotateAnim)
        errorText.visibility = View.GONE
        urlEditorLayout.visibility = View.GONE

        lifecycleScope.launch {
            if (intent.getBooleanExtra(ExtraBundleConst.FORCE_URL_DIALOG, false)) {
                showUrlEditor()
                return@launch;
            }
            val connected = checkServerConnection(url)
            if (connected) {
                delay(500)
                goToMain(url)
            } else {
                showUrlEditor()
            }
        }
    }

    private fun getUnsafeOkHttpClient(timeoutSeconds: Long = 5L): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {}

                override fun checkServerTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {}

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        val sslSocketFactory = sslContext.socketFactory

        return OkHttpClient.Builder()
            .sslSocketFactory(
                sslSocketFactory,
                trustAllCerts[0] as X509TrustManager
            )
            .connectTimeout(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private suspend fun checkServerConnection(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = url.toUri().buildUpon()
                .path(resources.getString(R.string.apiRelUrl))
                .build()
            
            val timeoutSeconds = try {
                prefs.getString(SettingsConst.API_TIMEOUT, "30")?.toLong() ?: 30L
            } catch (e: Exception) {
                30L
            }
            
            val client = getUnsafeOkHttpClient(timeoutSeconds)

            val request = Request.Builder().url(uri.toString()).build()
            val response = client.newCall(request).execute()
            
            if (response.code == 200) {
                val bodyString = response.body?.string() ?: return@withContext false
                val jsonArray = JSONArray(bodyString)
                
                if (jsonArray.length() == 0) return@withContext false
                
                val firstItem = jsonArray.getJSONObject(0)
                firstItem.has("Name") && 
                        firstItem.has("FrequencyActive") && 
                        firstItem.has("FrequencyStandby")
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun showUrlEditor() {
        loadingImage.clearAnimation()
        loadingImage.visibility = View.GONE
        errorText.visibility = View.VISIBLE
        errorText.text = resources.getString(R.string.connection_error_info_text, urlEditText.text.toString())
        urlEditorLayout.visibility = View.VISIBLE

        retryButton.setOnClickListener {
            val currentUrl = fixUrl(urlEditText.text.toString().trim())
            startConnectionCheck(currentUrl)
        }

        saveButton.setOnClickListener {
            val newUrl = fixUrl(urlEditText.text.toString().trim())
            urlEditText.setText(newUrl)
            prefs.edit().putString(SettingsConst.APP_URL, newUrl).apply()
            startConnectionCheck(newUrl)
        }
    }

    private fun fixUrl(url: String): String {
        val uri = url.toUri()
        if (uri.path?.isEmpty() != true) {
            return url
        }
        return uri.buildUpon().path(resources.getString(R.string.defaultUrlPath)).build().toString()
    }

    private fun goToMain(url: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.BundleExtraParamsConst.APP_URL, url)
        }
        startActivity(intent)
        finish()
    }
}