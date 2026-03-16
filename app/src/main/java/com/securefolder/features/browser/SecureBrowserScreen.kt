package com.securefolder.features.browser

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.securefolder.ui.theme.*

/**
 * Secure private browser.
 * No history, no cookies, no cache persisted. HTTPS-only mode.
 * JavaScript control, user-agent spoofing, and basic tracker blocking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureBrowserScreen(onBack: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isHttpsOnly by remember { mutableStateOf(true) }
    var isJavaScriptEnabled by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("Secure Browser") }

    // Tracker/ad domains to block
    val blockedDomains = remember {
        setOf(
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "google-analytics.com", "googletagmanager.com", "facebook.net",
            "facebook.com/tr", "analytics.twitter.com", "ads.twitter.com",
            "amazon-adsystem.com", "adnxs.com", "adsrvr.org",
            "taboola.com", "outbrain.com", "criteo.com",
            "tracking.", "tracker.", "pixel.", "beacon.",
            "ads.", "ad.", "analytics."
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            // Clear all browsing data on exit
            webView?.let { wv ->
                wv.clearHistory()
                wv.clearCache(true)
                wv.clearFormData()
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
                wv.destroy()
            }
        }
    }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🌐 Secure Browser",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // JavaScript toggle
                    IconButton(onClick = {
                        isJavaScriptEnabled = !isJavaScriptEnabled
                        webView?.settings?.javaScriptEnabled = isJavaScriptEnabled
                        webView?.reload()
                    }) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = "JavaScript",
                            tint = if (isJavaScriptEnabled) OrangeWarning else GreenSecure
                        )
                    }
                    // HTTPS only toggle
                    IconButton(onClick = { isHttpsOnly = !isHttpsOnly }) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "HTTPS Only",
                            tint = if (isHttpsOnly) GreenSecure else RedAlert
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Security indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecurityBadge("No History", GreenSecure)
                SecurityBadge("No Cookies", GreenSecure)
                SecurityBadge(
                    if (isHttpsOnly) "HTTPS Only" else "HTTP Allowed",
                    if (isHttpsOnly) GreenSecure else OrangeWarning
                )
                SecurityBadge(
                    if (isJavaScriptEnabled) "JS ON" else "JS OFF",
                    if (isJavaScriptEnabled) OrangeWarning else GreenSecure
                )
            }

            // URL bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantDark)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CyanAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                BasicTextField(
                    value = url,
                    onValueChange = { url = it },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(CyanAccent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            var loadUrl = url.trim()
                            if (!loadUrl.startsWith("http://") && !loadUrl.startsWith("https://")) {
                                loadUrl = if (loadUrl.contains(".")) {
                                    "https://$loadUrl"
                                } else {
                                    "https://duckduckgo.com/?q=${loadUrl.replace(" ", "+")}"
                                }
                            }
                            if (isHttpsOnly && loadUrl.startsWith("http://")) {
                                loadUrl = loadUrl.replace("http://", "https://")
                            }
                            webView?.loadUrl(loadUrl)
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box {
                            if (url.isEmpty()) {
                                Text(
                                    text = "Search or enter URL",
                                    style = TextStyle(fontSize = 14.sp, color = TextTertiary)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // WebView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        @SuppressLint("SetJavaScriptEnabled")
                        settings.apply {
                            javaScriptEnabled = isJavaScriptEnabled
                            domStorageEnabled = false
                            databaseEnabled = false
                            cacheMode = WebSettings.LOAD_NO_CACHE
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            allowFileAccess = false
                            allowContentAccess = false
                            setGeolocationEnabled(false)
                            // Spoofed user agent for privacy
                            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        // Disable cookies
                        CookieManager.getInstance().setAcceptCookie(false)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val requestUrl = request?.url?.toString() ?: return false

                                // Block trackers/ads
                                if (blockedDomains.any { requestUrl.contains(it) }) {
                                    return true
                                }

                                // HTTPS-only enforcement
                                if (isHttpsOnly && requestUrl.startsWith("http://")) {
                                    view?.loadUrl(requestUrl.replace("http://", "https://"))
                                    return true
                                }

                                return false
                            }

                            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                                isLoading = false
                                currentUrl = loadedUrl ?: ""
                                url = loadedUrl ?: ""
                                pageTitle = view?.title ?: "Secure Browser"
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: return null
                                // Block known trackers
                                if (blockedDomains.any { reqUrl.contains(it) }) {
                                    return WebResourceResponse(
                                        "text/plain", "utf-8",
                                        "".byteInputStream()
                                    )
                                }
                                return null
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                isLoading = newProgress < 100
                            }
                        }

                        webView = this

                        // Load default page
                        loadUrl("https://duckduckgo.com/")
                    }
                }
            )
        }
    }
}

@Composable
private fun SecurityBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = "● $text",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontSize = 9.sp
    )
}
