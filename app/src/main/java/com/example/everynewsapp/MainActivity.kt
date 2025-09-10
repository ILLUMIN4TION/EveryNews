package com.example.everynewsapp

import android.content.Intent // Intent import 추가
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.databinding.ActivityMainBinding
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.network.NaverNewsApi
import com.example.everynewsapp.ui.NewsAdapter
import com.example.everynewsapp.ui.TrendingNewsAdapter
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var newsList = mutableListOf<NewsItem>()
    private var trendingNewsList = mutableListOf<NewsItem>()

    private lateinit var newsAdapter: NewsAdapter
    private lateinit var trendingNewsAdapter: TrendingNewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupEventListeners()

        fetchNews("IT")
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(newsList)
        binding.rvDefaultNews.adapter = newsAdapter
        binding.rvDefaultNews.layoutManager = LinearLayoutManager(this)

        trendingNewsAdapter = TrendingNewsAdapter(trendingNewsList)
        binding.rvTrendingNews.adapter = trendingNewsAdapter
        binding.rvTrendingNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    // --- 이 함수가 수정되었습니다 ---
    private fun setupEventListeners() {
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    // 현재 화면이므로 아무것도 하지 않음
                    true
                }
                R.id.navigation_recommend -> { // '추천 뉴스' ID로 변경되었을 수 있습니다.
                    val intent = Intent(this, RecommendActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_scrap -> {
                    val intent = Intent(this, ScrapActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        binding.chipGroupCategory.findViewById<Chip>(R.id.chipPolitics).setOnClickListener {
            Toast.makeText(this, "정치 카테고리 선택", Toast.LENGTH_SHORT).show()
            fetchNews("정치")
        }
    }

    private fun fetchNews(query: String) {
        lifecycleScope.launch {
            val fetchedItems = NaverNewsApi.fetchNews(query)

            if (fetchedItems != null) {
                newsAdapter.updateData(fetchedItems)
                trendingNewsAdapter.updateData(fetchedItems)
            } else {
                Log.e("MainActivity", "뉴스 불러오기 실패 또는 결과 없음")
            }
        }
    }
}
