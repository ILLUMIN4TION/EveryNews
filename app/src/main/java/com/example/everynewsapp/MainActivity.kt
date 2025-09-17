package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.databinding.ActivityMainBinding
import com.example.everynewsapp.news.repository.NewsRepository
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.network.NaverNewsApi
import com.example.everynewsapp.news.database.AppDatabase // NewsDatabase import 추가
import com.example.everynewsapp.news.viewModel.NewsDetailViewModel
import com.example.everynewsapp.news.viewModel.NewsDetailViewModelFactory


import com.example.everynewsapp.ui.NewsAdapter
import com.example.everynewsapp.ui.TrendingNewsAdapter
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var trendingNewsAdapter: TrendingNewsAdapter
    private lateinit var newsDetailViewModel: NewsDetailViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ★★★ 1번 문제 해결: NewsRepository 생성자 매개변수 수정 ★★★
        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)

        val viewModelFactory = NewsDetailViewModelFactory(newsRepository)
        newsDetailViewModel = ViewModelProvider(this, viewModelFactory).get(NewsDetailViewModel::class.java)

        setupRecyclerView()
        setupEventListeners()

        fetchDefaultNews("최신 뉴스")
        fetchTrendingNews("인기 뉴스")
    }

    private fun setupRecyclerView() {
        // 이제 emptyList()를 전달하지 않아도 됩니다.
        newsAdapter = NewsAdapter(viewModel = newsDetailViewModel)
        binding.rvDefaultNews.adapter = newsAdapter
        binding.rvDefaultNews.layoutManager = LinearLayoutManager(this)

        trendingNewsAdapter = TrendingNewsAdapter(emptyList())
        binding.rvTrendingNews.adapter = trendingNewsAdapter
        binding.rvTrendingNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupEventListeners() {
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_recommend -> {
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
            fetchDefaultNews("정치")
        }
    }

    private fun fetchDefaultNews(query: String) {
        lifecycleScope.launch {
            val fetchedItems = NaverNewsApi.fetchNews(query)
            if (fetchedItems != null) {
                newsAdapter.updateData(fetchedItems)
            } else {
                Log.e("MainActivity", "기본 뉴스 불러오기 실패 또는 결과 없음")
            }
        }
    }

    private fun fetchTrendingNews(query: String) {
        lifecycleScope.launch {
            val fetchedItems = NaverNewsApi.fetchNews(query)
            if (fetchedItems != null) {
                trendingNewsAdapter.updateData(fetchedItems)
            } else {
                Log.e("MainActivity", "트렌딩 뉴스 불러오기 실패 또는 결과 없음")
            }
        }
    }
}