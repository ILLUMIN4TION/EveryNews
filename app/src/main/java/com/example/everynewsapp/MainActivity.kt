package com.example.everynewsapp

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

    // 어댑터의 데이터 리스트를 바꾸기 위해 var로 변경하고, MutableList를 사용합니다.
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

        // 앱 시작 시 "IT" 뉴스를 불러옵니다. 추후 최신뉴스로 수정하고 카테고리를 통해 분야를 바꾸도록 수정
        fetchNews("IT")
    }

    private fun setupRecyclerView() {
        // 어댑터를 초기화할 때 MutableList를 전달합니다.
        newsAdapter = NewsAdapter(newsList)
        binding.rvDefaultNews.adapter = newsAdapter
        binding.rvDefaultNews.layoutManager = LinearLayoutManager(this)

        trendingNewsAdapter = TrendingNewsAdapter(trendingNewsList)
        binding.rvTrendingNews.adapter = trendingNewsAdapter
        binding.rvTrendingNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupEventListeners() {
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    Toast.makeText(this, "홈 선택", Toast.LENGTH_SHORT).show()
                    true
                }
                // ... (다른 메뉴 아이템)
                else -> false
            }
        }

        binding.chipGroupCategory.findViewById<Chip>(R.id.chipPolitics).setOnClickListener {
            Toast.makeText(this, "정치 카테고리 선택", Toast.LENGTH_SHORT).show()
            fetchNews("정치") // 클릭 시 해당 카테고리 뉴스 불러오기,
        }
    }

    /**
     * 네이버 뉴스 API를 통해 뉴스를 가져오는 함수 (코루틴 방식으로 수정됨)
     */
    private fun fetchNews(query: String) {
        // lifecycleScope를 사용해 Activity 생명주기에 안전한 코루틴을 시작합니다.
        lifecycleScope.launch {
            // suspend 함수인 NaverNewsApi.fetchNews를 호출합니다.
            val fetchedItems = NaverNewsApi.fetchNews(query)

            if (fetchedItems != null) {
                // 어댑터의 데이터를 업데이트하는 효율적인 방식으로 변경
                newsAdapter.updateData(fetchedItems)
                trendingNewsAdapter.updateData(fetchedItems)
            } else {
                Log.e("MainActivity", "뉴스 불러오기 실패 또는 결과 없음")
            }
        }
    }
}
