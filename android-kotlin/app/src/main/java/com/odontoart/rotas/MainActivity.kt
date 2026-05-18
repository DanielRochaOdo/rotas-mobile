package com.odontoart.rotas

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var loadingBar: LinearProgressIndicator
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBar: AppBarLayout
    private lateinit var bottomNavigation: BottomNavigationView

    private var isUpdatingBottomNav = false
    private var isDarkThemeActive = false
    private var allowedBaseRoutes: Set<String>? = null

    private val primaryRoutes = mapOf(
        R.id.nav_dashboard to "/",
        R.id.nav_agenda to "/agenda",
        R.id.nav_visitas to "/visitas",
        R.id.nav_clientes to "/clientes",
    )

    private val moreRoutes = mapOf(
        R.id.nav_more_aceite to "/aceite-digital",
        R.id.nav_more_fila to "/fila",
        R.id.nav_more_kpi to "/kpi",
        R.id.nav_more_logs to "/logs",
        R.id.nav_more_configuracoes to "/configuracoes",
    )

    private inner class AndroidBridge {
        @JavascriptInterface
        fun onRouteChanged(rawRoute: String?) {
            runOnUiThread {
                updateRouteUi(rawRoute.orEmpty())
            }
        }

        @JavascriptInterface
        fun onThemeChanged(themeMode: String?) {
            runOnUiThread {
                applyNativeTheme(themeMode.equals("dark", ignoreCase = true))
            }
        }

        @JavascriptInterface
        fun onAllowedRoutesChanged(rawRoutesJson: String?) {
            runOnUiThread {
                val parsedRoutes = parseAllowedRoutes(rawRoutesJson)
                if (parsedRoutes.isEmpty() || parsedRoutes == allowedBaseRoutes) return@runOnUiThread
                allowedBaseRoutes = parsedRoutes
                applyNavigationVisibility()
                requestCurrentRoute()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.rootContainer)
        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        loadingBar = findViewById(R.id.loadingBar)
        toolbar = findViewById(R.id.topToolbar)
        appBar = findViewById(R.id.appBar)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ) = assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val host = uri.host.orEmpty()
                val scheme = uri.scheme.orEmpty()
                val isInternalHost = host == "appassets.androidplatform.net"
                val isHttpOrHttps = scheme == "http" || scheme == "https"

                if (isInternalHost) return false
                if (scheme.isEmpty() || scheme == "about" || scheme == "javascript") return false

                if (isHttpOrHttps) {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    return true
                }

                return runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                }.getOrElse { error ->
                    if (error !is ActivityNotFoundException) throw error
                    true
                }
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadingBar.isVisible = true
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                loadingBar.isVisible = false
                swipeRefresh.isRefreshing = false
                injectAndroidUxLayer()
                injectRouteObserver()
                requestCurrentRoute()
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = true
        webView.settings.setSupportZoom(false)
        webView.settings.builtInZoomControls = false
        webView.settings.displayZoomControls = false
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.settings.userAgentString = "${webView.settings.userAgentString} OdontoartAndroid/1.0"
        webView.overScrollMode = View.OVER_SCROLL_NEVER

        swipeRefresh.setColorSchemeColors(0xFF0C6F3D.toInt())
        swipeRefresh.setProgressBackgroundColorSchemeColor(0xFFFFFFFF.toInt())
        swipeRefresh.setOnChildScrollUpCallback { _, _ -> webView.scrollY > 0 }
        swipeRefresh.setOnRefreshListener { webView.reload() }

        bottomNavigation.setOnItemSelectedListener { item ->
            if (isUpdatingBottomNav) return@setOnItemSelectedListener true
            if (item.itemId == R.id.nav_more) {
                showMoreSectionsMenu(bottomNavigation)
                return@setOnItemSelectedListener false
            }

            val route = primaryRoutes[item.itemId] ?: return@setOnItemSelectedListener false
            if (!isRouteAllowed(route)) {
                showNoPermissionMessage()
                return@setOnItemSelectedListener false
            }
            navigateToRoute(route)
            true
        }

        bottomNavigation.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_more) {
                showMoreSectionsMenu(bottomNavigation)
                return@setOnItemReselectedListener
            }
            primaryRoutes[item.itemId]?.let { route ->
                if (!isRouteAllowed(route)) {
                    showNoPermissionMessage()
                    return@setOnItemReselectedListener
                }
                navigateToRoute(route, force = true)
                webView.evaluateJavascript("window.scrollTo({ top: 0, behavior: 'smooth' });", null)
            }
        }

        applyNativeTheme(isDark = false)
        applyNavigationVisibility()

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val tappable = insets.getInsets(WindowInsetsCompat.Type.tappableElement())
            val bottomInset = maxOf(bars.bottom, tappable.bottom)

            appBar.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            swipeRefresh.updatePadding(left = bars.left, right = bars.right)
            bottomNavigation.updatePadding(left = bars.left, right = bars.right, bottom = bottomInset)

            insets
        }
        ViewCompat.requestApplyInsets(root)

        if (savedInstanceState == null) {
            webView.loadUrl("https://appassets.androidplatform.net/index.html")
        }
    }

    private fun showMoreSectionsMenu(anchor: View) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_more_sections, popupMenu.menu)
        moreRoutes.forEach { (itemId, route) ->
            popupMenu.menu.findItem(itemId)?.isVisible = isRouteAllowed(route)
        }
        if (!popupMenu.menu.hasVisibleItems()) {
            showNoPermissionMessage()
            return
        }
        popupMenu.setOnMenuItemClickListener { item ->
            val route = moreRoutes[item.itemId] ?: return@setOnMenuItemClickListener false
            if (!isRouteAllowed(route)) {
                showNoPermissionMessage()
                return@setOnMenuItemClickListener false
            }
            navigateToRoute(route)
            true
        }
        popupMenu.show()
    }

    private fun navigateToRoute(route: String, force: Boolean = false) {
        if (!isRouteAllowed(route)) {
            showNoPermissionMessage()
            return
        }

        val escapedRoute = route
            .replace("\\", "\\\\")
            .replace("'", "\\'")

        val script = """
            (function () {
              try {
                const target = '$escapedRoute';
                const forceNavigation = ${if (force) "true" else "false"};
                const normalize = (path) => {
                  if (!path || path === '/index.html') return '/';
                  let normalized = path.startsWith('/') ? path : '/' + path;
                  normalized = normalized.split('?')[0].split('#')[0];
                  if (normalized.length > 1 && normalized.endsWith('/')) {
                    normalized = normalized.slice(0, -1);
                  }
                  return normalized || '/';
                };
                const hrefToRoute = (href) => {
                  if (!href) return null;
                  let value = String(href).trim();
                  if (
                    !value ||
                    value === '#' ||
                    value.startsWith('javascript:') ||
                    value.startsWith('mailto:') ||
                    value.startsWith('tel:')
                  ) {
                    return null;
                  }
                  if (value.startsWith('http://') || value.startsWith('https://')) {
                    try {
                      const absolute = new URL(value, window.location.origin);
                      if (absolute.origin !== window.location.origin) return null;
                      value = absolute.pathname + absolute.search + absolute.hash;
                    } catch (_) {
                      return null;
                    }
                  }
                  if (value.startsWith('#/')) return normalize(value.slice(1));
                  if (value.startsWith('#')) return null;
                  if (!value.startsWith('/')) return null;
                  return normalize(value);
                };

                const desiredRoute = normalize(target);
                const matchingLink = Array.from(document.querySelectorAll('a[href]')).find((anchor) => {
                  const candidateRoute = hrefToRoute(anchor.getAttribute('href') || anchor.href || '');
                  return candidateRoute === desiredRoute;
                });

                if (matchingLink) {
                  matchingLink.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
                  if (!forceNavigation) return;
                }

                const hasHashRouterSignal =
                  document.querySelector('a[href^="#/"]') !== null ||
                  (typeof window.location.hash === 'string' && window.location.hash.startsWith('#/'));
                if (hasHashRouterSignal) {
                  const wantedHash = '#' + desiredRoute;
                  if (forceNavigation || window.location.hash !== wantedHash) {
                    window.location.hash = wantedHash;
                  }
                  return;
                }

                const currentPath = normalize(window.location.pathname || '/');
                if (forceNavigation || currentPath !== desiredRoute) {
                  window.history.pushState({}, '', desiredRoute);
                  window.dispatchEvent(new PopStateEvent('popstate'));
                }
              } catch (_) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    private fun injectRouteObserver() {
        val script = """
            (function () {
              try {
                if (window.__odontoartAndroidBridgeInstalled) {
                  if (window.__odontoartNotifyRoute) {
                    window.__odontoartNotifyRoute();
                  }
                  if (window.__odontoartNotifyTheme) {
                    window.__odontoartNotifyTheme();
                  }
                  if (window.__odontoartNotifyAllowedRoutes) {
                    window.__odontoartNotifyAllowedRoutes();
                  }
                  return;
                }

                let lastRoute = null;
                let lastTheme = null;
                let lastAllowedRoutesKey = null;

                const normalizeRoute = () => {
                  const hash = window.location.hash || '';
                  if (hash.startsWith('#/')) {
                    return hash.slice(1);
                  }
                  let path = window.location.pathname || '/';
                  if (!path || path === '/index.html') path = '/';
                  return path;
                };

                const normalizePath = (path) => {
                  if (!path || path === '/index.html') return '/';
                  let normalized = path.startsWith('/') ? path : '/' + path;
                  normalized = normalized.split('?')[0].split('#')[0];
                  if (normalized.length > 1 && normalized.endsWith('/')) {
                    normalized = normalized.slice(0, -1);
                  }
                  return normalized || '/';
                };

                const hrefToRoute = (href) => {
                  if (!href) return null;
                  let value = String(href).trim();
                  if (
                    !value ||
                    value === '#' ||
                    value.startsWith('javascript:') ||
                    value.startsWith('mailto:') ||
                    value.startsWith('tel:')
                  ) {
                    return null;
                  }
                  if (value.startsWith('http://') || value.startsWith('https://')) {
                    try {
                      const absolute = new URL(value, window.location.origin);
                      if (absolute.origin !== window.location.origin) return null;
                      value = absolute.pathname + absolute.search + absolute.hash;
                    } catch (_) {
                      return null;
                    }
                  }
                  if (value.startsWith('#/')) return normalizePath(value.slice(1));
                  if (value.startsWith('#')) return null;
                  if (!value.startsWith('/')) return null;
                  return normalizePath(value);
                };

                const collectAllowedRoutes = () => {
                  const navRoots = document.querySelectorAll('aside, [role="navigation"]');
                  if (!navRoots || navRoots.length === 0) return [];

                  const seen = new Set();
                  const routes = [];
                  navRoots.forEach((root) => {
                    root.querySelectorAll('a[href]').forEach((anchor) => {
                      const route = hrefToRoute(anchor.getAttribute('href') || anchor.href || '');
                      if (!route || route === '/login') return;
                      if (!seen.has(route)) {
                        seen.add(route);
                        routes.push(route);
                      }
                    });
                  });
                  return routes.sort();
                };

                window.__odontoartNotifyRoute = () => {
                  try {
                    const route = normalizeRoute();
                    if (route === lastRoute) return;
                    lastRoute = route;
                    document.documentElement.setAttribute('data-odontoart-route', route);
                    if (document.body) {
                      document.body.setAttribute('data-odontoart-route', route);
                    }
                    if (window.AndroidBridge && window.AndroidBridge.onRouteChanged) {
                      window.AndroidBridge.onRouteChanged(route);
                    }
                  } catch (_) {}
                };

                window.__odontoartNotifyTheme = () => {
                  try {
                    const isDark =
                      document.documentElement.classList.contains('dark') ||
                      document.body.classList.contains('dark');
                    const currentTheme = isDark ? 'dark' : 'light';
                    if (currentTheme === lastTheme) return;
                    lastTheme = currentTheme;
                    if (window.AndroidBridge && window.AndroidBridge.onThemeChanged) {
                      window.AndroidBridge.onThemeChanged(currentTheme);
                    }
                  } catch (_) {}
                };

                window.__odontoartNotifyAllowedRoutes = () => {
                  try {
                    const routes = collectAllowedRoutes();
                    if (!routes.length) return;
                    const key = routes.join('|');
                    if (key === lastAllowedRoutesKey) return;
                    lastAllowedRoutesKey = key;
                    if (window.AndroidBridge && window.AndroidBridge.onAllowedRoutesChanged) {
                      window.AndroidBridge.onAllowedRoutesChanged(JSON.stringify(routes));
                    }
                  } catch (_) {}
                };

                window.addEventListener('popstate', window.__odontoartNotifyRoute);
                window.addEventListener('hashchange', window.__odontoartNotifyRoute);
                window.addEventListener('odontoart-theme-changed', window.__odontoartNotifyTheme);

                const observer = new MutationObserver(() => {
                  if (window.__odontoartNotifyTheme) {
                    window.__odontoartNotifyTheme();
                  }
                  if (window.__odontoartNotifyAllowedRoutes) {
                    window.__odontoartNotifyAllowedRoutes();
                  }
                });
                observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });
                observer.observe(document.body, { attributes: true, attributeFilter: ['class'] });

                window.__odontoartRouteThemeInterval = window.setInterval(() => {
                  if (window.__odontoartNotifyRoute) window.__odontoartNotifyRoute();
                  if (window.__odontoartNotifyTheme) window.__odontoartNotifyTheme();
                  if (window.__odontoartNotifyAllowedRoutes) window.__odontoartNotifyAllowedRoutes();
                }, 350);

                window.__odontoartAndroidBridgeInstalled = true;
                window.__odontoartNotifyRoute();
                window.__odontoartNotifyTheme();
                window.__odontoartNotifyAllowedRoutes();
              } catch (_) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    private fun requestCurrentRoute() {
        webView.evaluateJavascript(
            "(window.__odontoartNotifyRoute && window.__odontoartNotifyRoute());" +
                "(window.__odontoartNotifyTheme && window.__odontoartNotifyTheme());" +
                "(window.__odontoartNotifyAllowedRoutes && window.__odontoartNotifyAllowedRoutes());",
            null,
        )
    }

    private fun injectAndroidUxLayer() {
        val script = """
            (function () {
              try {
                if (window.__odontoartAndroidUxStyleApplied) return;

                var style = document.createElement('style');
                style.id = 'odontoart-android-shell-style';
                style.textContent = `
                  :root {
                    --android-shell-bg: #F6F7FB;
                    --android-shell-text: #0F172A;
                    --android-shell-muted: rgba(15, 23, 42, 0.74);
                    --android-shell-surface: rgba(255, 255, 255, 0.92);
                    --android-shell-surface-strong: rgba(255, 255, 255, 0.97);
                    --android-shell-surface-soft: rgba(239, 244, 247, 0.92);
                    --android-shell-border: rgba(12, 111, 61, 0.18);
                    --android-shell-border-strong: rgba(12, 111, 61, 0.28);
                    --android-shell-accent: #0C6F3D;
                    --android-shell-accent-soft: rgba(12, 111, 61, 0.12);
                    --android-shell-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
                  }
                  html.dark,
                  body.dark {
                    --android-shell-bg: #0B1220;
                    --android-shell-text: #F8FAFC;
                    --android-shell-muted: rgba(226, 232, 240, 0.78);
                    --android-shell-surface: rgba(15, 23, 42, 0.92);
                    --android-shell-surface-strong: rgba(17, 24, 39, 0.98);
                    --android-shell-surface-soft: rgba(30, 41, 59, 0.84);
                    --android-shell-border: rgba(148, 163, 184, 0.22);
                    --android-shell-border-strong: rgba(148, 163, 184, 0.34);
                    --android-shell-accent: #47C98B;
                    --android-shell-accent-soft: rgba(71, 201, 139, 0.18);
                    --android-shell-shadow: 0 22px 48px rgba(2, 6, 23, 0.34);
                  }
                  html, body {
                    overscroll-behavior-y: contain !important;
                    background: var(--android-shell-bg) !important;
                    color: var(--android-shell-text) !important;
                  }
                  body {
                    font-family: Roboto, "Noto Sans", sans-serif !important;
                  }
                  #root > div {
                    background: var(--android-shell-bg) !important;
                    color: var(--android-shell-text) !important;
                  }
                  #root > div > aside {
                    display: none !important;
                  }
                  #app-content-root {
                    margin: 0 !important;
                    border: 1px solid var(--android-shell-border) !important;
                    border-radius: 0 !important;
                    box-shadow: var(--android-shell-shadow) !important;
                    background: var(--android-shell-surface-strong) !important;
                    color: var(--android-shell-text) !important;
                  }
                  .text-ink,
                  .text-ink\\/80,
                  .text-ink\\/70,
                  .text-ink\\/60,
                  .text-ink\\/50 {
                    color: var(--android-shell-text) !important;
                  }
                  .text-muted,
                  html.dark .text-ink\\/50,
                  html.dark .text-ink\\/60,
                  html.dark .text-ink\\/70,
                  html.dark .text-muted {
                    color: var(--android-shell-muted) !important;
                  }
                  .bg-white,
                  .bg-white\\/95,
                  .bg-white\\/90,
                  .bg-white\\/80,
                  .bg-white\\/70,
                  .bg-sand\\/60,
                  .bg-sand\\/50,
                  .bg-sand\\/40,
                  .bg-sand\\/30,
                  .bg-sand\\/20 {
                    background: var(--android-shell-surface) !important;
                    color: var(--android-shell-text) !important;
                  }
                  .border-sea\\/15,
                  html.dark .border-sea\\/15,
                  .border-sea\\/20,
                  html.dark .border-sea\\/20,
                  .border-sea\\/25,
                  html.dark .border-sea\\/25 {
                    border-color: var(--android-shell-border) !important;
                  }
                  .border-sea\\/30,
                  .border-sea\\/35,
                  .border-sea\\/50 {
                    border-color: var(--android-shell-border-strong) !important;
                  }
                  .bg-sea\\/10,
                  .bg-sea\\/12,
                  .bg-sea\\/15,
                  .bg-sea\\/20 {
                    background: var(--android-shell-accent-soft) !important;
                  }
                  .text-sea,
                  .hover\\:text-sea:hover,
                  .hover\\:text-seaLight:hover {
                    color: var(--android-shell-accent) !important;
                  }
                  .hover\\:border-sea:hover,
                  .hover\\:border-sea\\/25:hover,
                  .hover\\:border-sea\\/35:hover,
                  .hover\\:border-sea\\/50:hover {
                    border-color: var(--android-shell-accent) !important;
                  }
                  input,
                  select,
                  textarea {
                    background: var(--android-shell-surface-strong) !important;
                    border-color: var(--android-shell-border) !important;
                    color: var(--android-shell-text) !important;
                    box-shadow: none !important;
                  }
                  input::placeholder,
                  textarea::placeholder {
                    color: var(--android-shell-muted) !important;
                  }
                  select option {
                    background: #0F172A;
                    color: #F8FAFC;
                  }
                  html:not(.dark) select option {
                    background: #FFFFFF;
                    color: #0F172A;
                  }
                  input:focus,
                  select:focus,
                  textarea:focus {
                    border-color: var(--android-shell-accent) !important;
                    outline: none !important;
                  }
                  [class*="shadow-card"],
                  [class*="shadow-lg"],
                  [class*="shadow-md"] {
                    box-shadow: var(--android-shell-shadow) !important;
                  }
                  [data-odontoart-route^="/rotas"] aside button,
                  [data-odontoart-route="/rotas"] aside button {
                    background: var(--android-shell-surface) !important;
                    border-color: var(--android-shell-border) !important;
                    color: var(--android-shell-text) !important;
                  }
                  [data-odontoart-route^="/rotas"] aside button[class*="bg-sea"],
                  [data-odontoart-route="/rotas"] aside button[class*="bg-sea"] {
                    background: var(--android-shell-accent-soft) !important;
                    border-color: var(--android-shell-accent) !important;
                  }
                  [data-odontoart-route^="/rotas"] section > div > div > h3,
                  [data-odontoart-route="/rotas"] section > div > div > h3 {
                    color: var(--android-shell-text) !important;
                  }
                  #root > div > div.flex.min-h-screen.w-full.flex-col {
                    padding: 0 !important;
                    gap: 0 !important;
                  }
                  @media (max-width: 767px) {
                    #root > div > div.md\\:hidden {
                      display: none !important;
                    }
                    #root > div > div.fixed.left-1\\/2 {
                      display: none !important;
                    }
                    #app-content-root {
                      padding: 12px 12px 16px !important;
                    }
                  }
                `;

                document.head.appendChild(style);
                window.__odontoartAndroidUxStyleApplied = true;
              } catch (_) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    private fun applyNativeTheme(isDark: Boolean) {
        isDarkThemeActive = isDark

        val appBarColor = if (isDark) Color.parseColor("#0F172A") else Color.WHITE
        val navColor = if (isDark) Color.parseColor("#111827") else Color.WHITE
        val titleColor = if (isDark) Color.parseColor("#E5E7EB") else Color.parseColor("#0F172A")
        val accentColor = if (isDark) Color.parseColor("#3CB179") else Color.parseColor("#0C6F3D")
        val unselectedColor = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#667085")
        val trackColor = if (isDark) Color.parseColor("#243244") else Color.parseColor("#CCE5D8")

        appBar.setBackgroundColor(appBarColor)
        toolbar.setBackgroundColor(appBarColor)
        toolbar.setTitleTextColor(titleColor)
        bottomNavigation.setBackgroundColor(navColor)

        val navColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ),
            intArrayOf(
                accentColor,
                unselectedColor,
            ),
        )

        bottomNavigation.itemIconTintList = navColorStateList
        bottomNavigation.itemTextColor = navColorStateList
        swipeRefresh.setColorSchemeColors(accentColor)
        loadingBar.setIndicatorColor(accentColor)
        loadingBar.trackColor = trackColor
    }

    private fun updateRouteUi(rawRoute: String) {
        val route = normalizeRoute(rawRoute)
        toolbar.setTitle(getTitleForRoute(route))
        if (route.startsWith("/login")) {
            bottomNavigation.isVisible = false
            return
        }

        applyNavigationVisibility()
        if (!bottomNavigation.isVisible) return

        val destination = when {
            route == "/" -> R.id.nav_dashboard
            route.startsWith("/agenda") || route.startsWith("/rotas") -> R.id.nav_agenda
            route.startsWith("/visitas") -> R.id.nav_visitas
            route.startsWith("/clientes") -> R.id.nav_clientes
            route.startsWith("/aceite-digital") ||
                route.startsWith("/fila") ||
                route.startsWith("/kpi") ||
                route.startsWith("/logs") ||
                route.startsWith("/configuracoes") -> R.id.nav_more
            else -> 0
        }

        if (destination != 0 &&
            bottomNavigation.menu.findItem(destination)?.isVisible == true &&
            bottomNavigation.selectedItemId != destination
        ) {
            isUpdatingBottomNav = true
            bottomNavigation.selectedItemId = destination
            isUpdatingBottomNav = false
        }
    }

    private fun getTitleForRoute(route: String): Int = when {
        route.startsWith("/login") -> R.string.title_login
        route == "/" -> R.string.title_dashboard
        route.startsWith("/agenda") || route.startsWith("/rotas") -> R.string.title_agenda
        route.startsWith("/visitas") -> R.string.title_visitas
        route.startsWith("/clientes") -> R.string.title_clientes
        route.startsWith("/aceite-digital") -> R.string.title_aceite
        route.startsWith("/fila") -> R.string.title_fila
        route.startsWith("/kpi") -> R.string.title_kpi
        route.startsWith("/logs") -> R.string.title_logs
        route.startsWith("/configuracoes") -> R.string.title_configuracoes
        else -> R.string.app_name
    }

    private fun normalizeRoute(rawRoute: String): String {
        val trimmed = rawRoute.trim()
        if (trimmed.isEmpty()) return "/"

        val withoutHash = if (trimmed.startsWith("#")) trimmed.removePrefix("#") else trimmed
        val withLeadingSlash = when {
            withoutHash.isEmpty() -> "/"
            withoutHash == "index.html" || withoutHash == "/index.html" -> "/"
            withoutHash.startsWith("/") -> withoutHash
            else -> "/$withoutHash"
        }

        val normalized = withLeadingSlash
            .substringBefore("?")
            .substringBefore("#")
            .ifEmpty { "/" }

        return if (normalized.length > 1) normalized.trimEnd('/') else normalized
    }

    private fun baseRoute(route: String): String {
        val normalized = normalizeRoute(route)
        return when {
            normalized == "/" -> "/"
            normalized.startsWith("/agenda") || normalized.startsWith("/rotas") -> "/agenda"
            normalized.startsWith("/visitas") -> "/visitas"
            normalized.startsWith("/clientes") -> "/clientes"
            normalized.startsWith("/aceite-digital") -> "/aceite-digital"
            normalized.startsWith("/fila") -> "/fila"
            normalized.startsWith("/kpi") -> "/kpi"
            normalized.startsWith("/logs") -> "/logs"
            normalized.startsWith("/configuracoes") -> "/configuracoes"
            normalized.startsWith("/login") -> "/login"
            else -> {
                val firstSegment = normalized.removePrefix("/").substringBefore("/")
                if (firstSegment.isBlank()) "/" else "/$firstSegment"
            }
        }
    }

    private fun parseAllowedRoutes(rawRoutesJson: String?): Set<String> {
        if (rawRoutesJson.isNullOrBlank()) return emptySet()

        return runCatching {
            val routes = linkedSetOf<String>()
            val json = JSONArray(rawRoutesJson)
            for (index in 0 until json.length()) {
                val value = json.optString(index).orEmpty()
                if (value.isBlank()) continue
                val base = baseRoute(value)
                if (base != "/login") {
                    routes.add(base)
                }
            }
            routes
        }.getOrDefault(emptySet())
    }

    private fun isRouteAllowed(route: String): Boolean {
        val allowed = allowedBaseRoutes ?: return true
        val base = baseRoute(route)
        if (base == "/login") return true
        return allowed.contains(base)
    }

    private fun applyNavigationVisibility() {
        val menu = bottomNavigation.menu

        primaryRoutes.forEach { (itemId, route) ->
            menu.findItem(itemId)?.isVisible = isRouteAllowed(route)
        }
        val hasVisibleMoreRoutes = moreRoutes.any { (_, route) -> isRouteAllowed(route) }
        menu.findItem(R.id.nav_more)?.isVisible = hasVisibleMoreRoutes

        val hasVisibleNavigationItems = (0 until menu.size()).any { index ->
            menu.getItem(index).isVisible
        }
        bottomNavigation.isVisible = hasVisibleNavigationItems

        val currentSelectedItem = menu.findItem(bottomNavigation.selectedItemId)
        if (currentSelectedItem == null || !currentSelectedItem.isVisible) {
            val firstVisiblePrimary = primaryRoutes.keys.firstOrNull { itemId ->
                menu.findItem(itemId)?.isVisible == true
            }
            if (firstVisiblePrimary != null) {
                isUpdatingBottomNav = true
                bottomNavigation.selectedItemId = firstVisiblePrimary
                isUpdatingBottomNav = false
            }
        }
    }

    private fun showNoPermissionMessage() {
        Toast.makeText(this, R.string.nav_no_permission, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
