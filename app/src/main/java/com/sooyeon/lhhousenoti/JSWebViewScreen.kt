package com.sooyeon.lhhousenoti

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.sooyeon.lhhousenoti.Manager.NotificationManager
import com.sooyeon.lhhousenoti.Model.LHHouseModel
import org.json.JSONArray
import org.json.JSONObject

// ------------------------------------------------------------------
// 💡 MutationObserver 무한 루프 방지 로직 추가
// ------------------------------------------------------------------
private const val INJECTED_JS = """
(function() {
    if (window.__lhBridgeInjected) return;
    window.__lhBridgeInjected = true;

    var styleNode = document.createElement('style');
    styleNode.type = 'text/css';
    var cssRules = '#mNav, #header, .subHeader { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; height: 0 !important; width: 0 !important; }';
    styleNode.innerHTML = cssRules;
    document.documentElement.appendChild(styleNode);

    // MutationObserver는 스타일 주입만으로 해결되지 않을 때만 최소한으로 사용하거나 일단 제거하여 터치 프리징 방지
    /*
    var observer = new MutationObserver(function(mutations) {
        ...
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });
    */

    document.addEventListener('click', function(event) {
        var target = event.target;
        while (target && target !== document) {
            var isLHBack = target.classList && (target.classList.contains('btn_back') ||
                           target.classList.contains('ico_back')) ||
                           target.id === 'btnBack' ||
                           target.getAttribute('aria-label') === '이전화면' ||
                           (target.textContent && target.textContent.trim() === '목록');

            var href = target.getAttribute ? (target.getAttribute('href') || '') : '';
            var isJsHistoryBack = href.toLowerCase().includes('javascript:history.back');

            if (isLHBack || isJsHistoryBack) {
                window.AndroidBridge.postMessage(JSON.stringify({ action: 'clickHeaderBackButton' }));
                break;
            }
            target = target.parentNode;
        }
    }, true);

    document.addEventListener('click', function(event) {
        var target = event.target;
        while (target && target !== document) {
            var href = target.getAttribute ? (target.getAttribute('href') || '') : '';
            if (href.toLowerCase().includes('javascript:saveitrpan')) {
                window.AndroidBridge.postMessage(JSON.stringify({ action: 'clickSaveItrPan' }));
                break;
            }
            target = target.parentNode;
        }
    }, true);

    document.addEventListener('click', function(event) {
        var target = event.target;
        while (target && target !== document) {
            var onclickStr = target.getAttribute ? (target.getAttribute('onclick') || '') : '';
            if (onclickStr.includes('docViewer')) {
                var match = onclickStr.match(/docViewer\s*\(([^)]+)\)/);
                if (match && match[1]) {
                    var params = match[1].split(',').map(function(param) {
                        return param.trim().replace(/^['"]|['"]$/g, '');
                    });
                    window.AndroidBridge.postMessage(JSON.stringify({
                        action: 'clickDocViewer',
                        params: params
                    }));
                }
                break;
            }
            target = target.parentNode;
        }
    }, true);
})();
"""

private const val REINFORCE_HIDE_JS = """
(function() {
    var footerNav = document.getElementById('mNav');
    if (footerNav) { footerNav.style.setProperty('display', 'none', 'important'); }

    var header = document.getElementById('header');
    if (header) { header.style.setProperty('display', 'none', 'important'); }

    if (document.body) {
        document.body.style.setProperty('padding-bottom', '0px', 'important');
        document.body.style.setProperty('padding-top', '0px', 'important');
    }
})();
"""

private class AndroidBridge(
    private val webView: WebView,
    private val onAction: (action: String, body: JSONObject?, webView: WebView) -> Unit,
) {
    @JavascriptInterface
    @Suppress("unused")
    fun postMessage(message: String) {
        runCatching {
            val json = JSONObject(message)
            val action = json.optString("action", "")
            if (action.isNotEmpty()) {
                onAction(action, json, webView)
            }
        }
    }

    @JavascriptInterface
    fun deviceInfoReq() {
        onAction("deviceInfoReq", null, webView)
    }

    @JavascriptInterface
    fun openWebView(message: String) {
        runCatching {
            val json = JSONObject(message)
            onAction("openWebView", json, webView)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JSWebViewScreen(
    url: String,
    modifier: Modifier = Modifier,
    onNavigateToDetail: (LHHouseModel, String) -> Unit,
    onCloseExpandWebView: () -> Unit = {},
    onOpenHouseDetail: (LHHouseModel) -> Unit = {}
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // 터치 및 스크롤 반응성 확보
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                requestFocus()

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true

                    // 추가 설정
                    databaseEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                }

                webChromeClient = WebChromeClient()

                addJavascriptInterface(
                    AndroidBridge(this) { action, body, view ->
                        view.post {
                            handleBridgeAction(
                                context = context,
                                action = action,
                                body = body,
                                webView = view,
                                onNavigateToDetail = onNavigateToDetail,
                                onCloseExpandWebView = onCloseExpandWebView,
                                onOpenHouseDetail = onOpenHouseDetail,
                                onOpenDocViewer = { params -> openDocViewer(context, params) }
                            )
                        }
                    },
                    "AndroidBridge"
                )

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, pageUrl, favicon)
                        Log.d("JSWebViewScreen", "onPageStarted: $pageUrl")
                        view?.evaluateJavascript(INJECTED_JS, null)
                        view?.visibility = View.GONE
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        Log.d("JSWebViewScreen", "onPageFinished: $pageUrl")
                        view?.evaluateJavascript(INJECTED_JS, null)
                        view?.evaluateJavascript(REINFORCE_HIDE_JS, null)
                        view?.visibility = View.VISIBLE
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val requestUrl = request?.url?.toString() ?: ""
                        Log.d("JSWebViewScreen", "shouldOverrideUrlLoading: $requestUrl")
                        
                        // URL에 dtlUrl이 포함되어 있거나 특정 상세 패턴인 경우 처리
                        if (requestUrl.contains("dtlUrl=") || requestUrl.contains("detail")) {
                            onNavigateToDetail(LHHouseModel(), requestUrl)
                            return true
                        }
                        return false
                    }
                }

                loadUrl(url)
            }
        },
        update = { view ->
            // URL이 변경되었을 때만 새로운 페이지 로드
            if (view.url != url) {
                view.loadUrl(url)
            }
        }
    )
}

private fun handleBridgeAction(
    context: Context,
    action: String,
    body: JSONObject?,
    webView: WebView,
    onNavigateToDetail: (LHHouseModel, String) -> Unit,
    onCloseExpandWebView: () -> Unit,
    onOpenHouseDetail: (LHHouseModel) -> Unit,
    onOpenDocViewer: (List<String>) -> Unit
) {
    when (action) {
        "clickHeaderBackButton" -> onCloseExpandWebView()
        "clickSaveItrPan" -> { }
        "clickDocViewer" -> {
            val paramsArray: JSONArray? = body?.optJSONArray("params")
            if (paramsArray != null && (paramsArray.length() > 2)) {
                val params = (0 until paramsArray.length()).map { paramsArray.optString(it) }
                onOpenDocViewer(params)
            }
        }
        "openWebView" -> {
            Log.d("JSWebViewScreen", "openWebView action received: $body")
            body?.let {
                LHHouseModel.fromJson(it.toString())?.let { model ->
                    onOpenHouseDetail(model)

                    val requestUrl = model.dtlUrl ?: ""
                    Log.d("JSWebViewScreen", "Navigating to detail: $requestUrl")
                    onNavigateToDetail(model, requestUrl)
                }
            }
        }
        "deviceInfoReq" -> {
            sendDeviceInfoToWeb(context, webView)
        }
    }
}

private fun openDocViewer(context: Context, params: List<String>) {
    if (params.size < 3) return
    val filepath = params[0]
    val filename = params[1]
    val fileext = params[2]

    val urlString =
        "https://apply.lh.or.kr/view/viewer/document/docviewer.do?filepath=$filepath&filename=$filename&fileext=$fileext"

    val customTabsIntent = CustomTabsIntent.Builder().build()
    customTabsIntent.launchUrl(context, urlString.toUri())
}

private fun sendDeviceInfoToWeb(context: Context, webView: WebView) {
    NotificationManager.getPushToken { token ->
        val uuid = DeviceIdentifier.getDeviceUUID(context)
        val ostype = "a"
        val modelname = Build.MODEL
        val detailmodelname = Build.PRODUCT
        val safeToken = token ?: ""

        webView.post {
            // iOS처럼 5개의 인자를 각각 따옴표로 감싸서 전달
            val script = "javascript:if(window.receiveDeviceInfo) { " +
                    "window.receiveDeviceInfo('$uuid', '$safeToken', '$ostype', '$modelname', '$detailmodelname'); " +
                    "}"

            webView.evaluateJavascript(script, null)
        }
    }
}