package com.example.everynewsapp.news.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.everynewsapp.R

class NewsWebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_webview)

        val webView: WebView = findViewById(R.id.newsWebView)

        val link = intent.getStringExtra("link") ?: ""

        // WebView 기본 설정
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true  // 일부 뉴스 페이지는 JS 필요

        // 원문 로드
        webView.loadUrl(link)
    }
}
