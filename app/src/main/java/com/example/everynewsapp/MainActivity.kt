package com.example.everynewsapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.network.NaverNewsApi
import com.example.everynewsapp.news.ui.NewsAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // RecyclerView 초기화
        recyclerView = findViewById(R.id.newsRecyclerView)
        adapter = NewsAdapter(emptyList())   // 초기엔 빈 리스트
        recyclerView.adapter = adapter

        // 뉴스 불러오기
        fetchNews("스마트폰")
    }

    private fun fetchNews(query: String) {
        NaverNewsApi.fetchNews(query) { newsResponse ->
            if (newsResponse != null) {
                val items: List<NewsItem> = newsResponse

                runOnUiThread {
                    adapter = NewsAdapter(items)
                    recyclerView.adapter = adapter
                }
            } else {
                Log.e("MainActivity", "뉴스 불러오기 실패")
            }
        }
    }
}
