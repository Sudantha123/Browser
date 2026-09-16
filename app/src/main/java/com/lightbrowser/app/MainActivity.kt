package com.lightbrowser.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    
    // Media controls සඳහා Service එකට කතා කිරීමට Action strings
    companion object {
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREV = "ACTION_PREV"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true // YouTube වැඩ කිරීමට අත්‍යවශ්‍යයි
        settings.mediaPlaybackRequiresUserGesture = false // Auto-play සඳහා ඉඩ දීම
        
        // Caching - RAM සහ Data ඉතිරි කිරීම
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

        // Background play වීමට YouTube Page Visibility API එක block කිරීම අවශ්‍යයි
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // App එක background ගියත් Youtube නවතින්නේ නැති වෙන්න JS inject කිරීම
                view?.evaluateJavascript(
                    """
                    Object.defineProperty(document, 'visibilityState', { get: function () { return 'visible'; } });
                    Object.defineProperty(document, 'hidden', { get: function () { return false; } });
                    """.trimIndent(), null
                )
            }
        }

        // Notification panel එකේ buttons එබූ විට WebView එකේ media පාලනය කිරීම
        webView.addJavascriptInterface(MediaWebInterface(), "AndroidMedia")

        // Download පහසුකම
        webView.setDownloadListener { url, _, _, _, _ ->
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent) // සරලව බාහිරින් download වීමට ඉඩ දීම (RAM වැය නොවේ)
        }

        webView.loadUrl("https://m.youtube.com")

        // Notification panel එකේ media controls සඳහා Service එක පණගැන්වීම
        val serviceIntent = Intent(this, MediaPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onPause() {
        super.onPause()
        // වැදගත්: WebView එකේ onPause() call කරන්නේ නැහැ. එතකොට සින්දු දිගටම play වෙනවා.
    }

    // Back button එබූ විට browser එක back වීම
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    // Javascript වලින් Android එකට කතා කරන interface එක
    inner class MediaWebInterface {
        @JavascriptInterface
        fun updateMetaData(title: String, artist: String) {
            // මෙතනින් Notification එකේ Title එක වෙනස් කරන්න Broadcast එකක් යැවිය හැක
        }
    }
}
