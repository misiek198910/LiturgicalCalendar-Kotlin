package mivs.liturgicalcalendar

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class PrivacyPolicyActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy_policy)
        changeNaviBarColor();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val webView = findViewById<WebView>(R.id.webView)
        webView.webViewClient = WebViewClient() 
        webView.settings.javaScriptEnabled = true 

        webView.loadUrl("https://misiek198910.github.io/mojaparafia-privacy/privacy_liturgy.html")
    }

    private fun changeNaviBarColor() {

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // 1. Wyłączamy jasny pasek nawigacji i włączamy ciemny
        controller.isAppearanceLightNavigationBars = false
        controller.isAppearanceLightStatusBars = false
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.BLACK
    }
}