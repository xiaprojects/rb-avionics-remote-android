package com.xiaprojects.rb

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ComponentActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class SplashScreenActivity : ComponentActivity() {

    private lateinit var loadingImage: ImageView
    private lateinit var errorText: TextView
    private lateinit var urlEditorLayout: View
    private lateinit var urlEditText: EditText
    private lateinit var retryButton: Button
    private lateinit var saveButton: Button
    private lateinit var rotateAnim: Animation

    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        //val splashScreen = SplashScreen.
        installSplashScreen().apply {
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

        // Load saved URL or default
        val savedUrl = prefs.getString("api_url", "https://your.api.server/health")!!
        urlEditText.setText(savedUrl)

        // Load animation
        rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate)

        startConnectionCheck(savedUrl)
    }

    private fun startConnectionCheck(url: String) {
        loadingImage.visibility = View.VISIBLE
        loadingImage.startAnimation(rotateAnim)
        errorText.visibility = View.GONE
        urlEditorLayout.visibility = View.GONE

        lifecycleScope.launch {
            val connected = checkServerConnection(url)
            if (connected) {
                delay(500)
                goToMain(url)
            } else {
                showUrlEditor()
            }
        }
    }

    private suspend fun checkServerConnection(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            response.code == 200
        } catch (e: Exception) {
            false
        }
    }

    private fun showUrlEditor() {
        loadingImage.clearAnimation()
        loadingImage.visibility = View.GONE
        errorText.visibility = View.VISIBLE
        urlEditorLayout.visibility = View.VISIBLE

        retryButton.setOnClickListener {
            val currentUrl = urlEditText.text.toString()
            startConnectionCheck(currentUrl)
        }

        saveButton.setOnClickListener {
            val newUrl = urlEditText.text.toString().trim()
            prefs.edit().putString("api_url", newUrl).apply()
            startConnectionCheck(newUrl)
        }
    }

    private fun goToMain(url: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("api_url", url)
        }
        startActivity(intent)
        finish()
    }
}