package com.sooyeon.lhhousenoti

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JSWebViewScreen(
    url: String,
    onNavigateToDetail: (String) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true // 자바스크립트 활성화
                    domStorageEnabled = true // 로컬 스토리지 등 사용 시 필요
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val requestUrl = request?.url?.toString() ?: ""
                        
                        // 특정 URL 패턴인 경우 상세 화면으로 네비게이션
                        // 예: detail_url 파라미터가 포함되어 있거나 특정 도메인일 경우 등
                        // 여기서는 단순히 전달받은 콜백을 어떻게 호출하는지 예시를 보여줍니다.
                        if (requestUrl.contains("detail")) {
                            onNavigateToDetail(requestUrl)
                            return true // WebView가 직접 로드하지 않음
                        }
                        
                        return false // WebView가 직접 로드
                    }
                }
                
                loadUrl(url)
            }
        },
        update = { webView ->
            // URL이 바뀌었을 때 업데이트가 필요한 경우 처리
            // webView.loadUrl(url)
        }
    )
}
