package com.example.everynewsapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // lifecycleScope 임포트
import androidx.recyclerview.widget.RecyclerView
import com.example.everynewsapp.news.data.AppDatabase
import com.example.everynewsapp.news.model.BookmarkedNews
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.network.NaverNewsApi
import com.example.everynewsapp.news.ui.NewsAdapter
import kotlinx.coroutines.Dispatchers // Dispatchers 임포트
import kotlinx.coroutines.launch // launch 임포트

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.newsRecyclerView)
        adapter = NewsAdapter(emptyList()) { item ->
            saveBookmark(item)
        }
        recyclerView.adapter = adapter

        fetchNews("스마트폰")
    }

    private fun fetchNews(query: String) {
        NaverNewsApi.fetchNews(query) { newsResponse ->
            if (newsResponse != null) {
                runOnUiThread {
                    adapter = NewsAdapter(newsResponse) { item ->
                        saveBookmark(item)
                    }
                    recyclerView.adapter = adapter
                }
            } else {
                Log.e("MainActivity", "뉴스 불러오기 실패")
            }
        }
    }

    private fun saveBookmark(item: NewsItem) {
        val db = AppDatabase.getDatabase(this)
        val dao = db.bookmarkedNewsDao()
        val bookmark = BookmarkedNews(
            title = item.title.replace("<b>", "").replace("</b>", ""),
            link = item.link,
            description = item.description.replace("<b>", "").replace("</b>", ""),
            pubDate = item.pubDate,
            thumbnail = null
        )

        // 코루틴을 사용하여 백그라운드에서 데이터베이스 작업 수행
        lifecycleScope.launch(Dispatchers.IO) {
            dao.insert(bookmark)
            // 필요하다면 UI 업데이트 (예: 북마크 성공 메시지)
            // withContext(Dispatchers.Main) {
            //     Toast.makeText(this@MainActivity, "북마크에 저장되었습니다.", Toast.LENGTH_SHORT).show()
            // }
        }
    }
}
