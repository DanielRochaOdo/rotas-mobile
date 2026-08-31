package com.odontoart.rotas

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.odontoart.rotas.update.AppUpdateManager

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private val appUpdateManager = AppUpdateManager()
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = filePathCallback ?: return@registerForActivityResult
        val uris = if (result.resultCode == Activity.RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        } else {
            null
        }
        callback.onReceiveValue(uris)
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.rgb(246, 250, 247))
            overScrollMode = WebView.OVER_SCROLL_NEVER

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mediaPlaybackRequiresUserGesture = true
            settings.userAgentString = "${settings.userAgentString} OdontoartAndroid/${BuildConfig.VERSION_NAME}"

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    val uri = request.url
                    if (shouldServeSpaIndex(uri, request.isForMainFrame)) {
                        return WebResourceResponse(
                            "text/html",
                            "UTF-8",
                            assets.open("index.html"),
                        )
                    }
                    return assetLoader.shouldInterceptRequest(uri)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = openExternalIfNeeded(request.url)
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?,
                ): Boolean {
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = filePathCallback
                    val chooserIntent = runCatching { fileChooserParams?.createIntent() }.getOrNull()
                    if (chooserIntent == null) {
                        this@MainActivity.filePathCallback?.onReceiveValue(null)
                        this@MainActivity.filePathCallback = null
                        return false
                    }
                    return runCatching {
                        fileChooserLauncher.launch(chooserIntent)
                        true
                    }.getOrElse {
                        this@MainActivity.filePathCallback?.onReceiveValue(null)
                        this@MainActivity.filePathCallback = null
                        false
                    }
                }
            }

            setDownloadListener { url, _, _, _, _ ->
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }
            }
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(246, 250, 247))
            addView(webView)
        }
        setContentView(root)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )

        if (savedInstanceState == null) {
            webView.loadUrl(APP_INDEX_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }

        appUpdateManager.checkForUpdate(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.resumePendingInstall(this)
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.webChromeClient = null
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun shouldServeSpaIndex(uri: Uri, isForMainFrame: Boolean): Boolean {
        if (!isForMainFrame || uri.host != APP_ASSET_HOST) return false
        val path = uri.path.orEmpty()
        if (path.isBlank() || path == "/" || path == "/index.html") return false
        val lastSegment = path.substringAfterLast('/')
        return !lastSegment.contains('.')
    }

    private fun openExternalIfNeeded(uri: Uri): Boolean {
        val scheme = uri.scheme.orEmpty().lowercase()
        val host = uri.host.orEmpty()
        if (scheme == "https" && host == APP_ASSET_HOST) return false
        if (scheme.isBlank() || scheme == "about" || scheme == "javascript") return false

        if (scheme == "intent") {
            return runCatching {
                val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                startActivity(intent)
                true
            }.getOrElse { true }
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)
        return try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            // O JavaScript do web possui fallback para URLs HTTP quando o app de mapa
            // nao esta instalado. Consumimos o deep link e deixamos o timeout do web agir.
            true
        }
    }

    private companion object {
        const val APP_ASSET_HOST = "appassets.androidplatform.net"
        const val APP_INDEX_URL = "https://$APP_ASSET_HOST/index.html"
    }
}
