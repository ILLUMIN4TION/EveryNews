package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.databinding.ActivityRecommendBinding
import com.example.everynewsapp.news.database.AppDatabase
import com.example.everynewsapp.news.repository.NewsRepository
import com.example.everynewsapp.news.viewModel.NewsViewModel
import com.example.everynewsapp.news.viewModel.NewsViewModelFactory
import com.example.everynewsapp.news.viewModel.RecommendNewsViewModel
import com.example.everynewsapp.news.viewModel.RecommendNewsViewModelFactory
import com.example.everynewsapp.ui.RecommendNewsAdapter

class RecommendActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecommendBinding
    private lateinit var recommendViewModel: RecommendNewsViewModel
    private lateinit var newsViewModel: NewsViewModel
    private lateinit var recommendNewsAdapter: RecommendNewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityRecommendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.selectedItemId = R.id.navigation_recommend

        // 1. Repository 및 ViewModel Factory 초기화
        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)

        // 2. 추천 뉴스 ViewModel 초기화
        val recommendViewModelFactory = RecommendNewsViewModelFactory(newsRepository)
        recommendViewModel = ViewModelProvider(this, recommendViewModelFactory).get(RecommendNewsViewModel::class.java)

        // 3. 스크랩 기능 ViewModel 초기화 (어댑터에 전달)
        val newsViewModelFactory = NewsViewModelFactory(newsRepository, application)
        newsViewModel = ViewModelProvider(this, newsViewModelFactory).get(NewsViewModel::class.java)

        setupRecyclerView()
        setupEventListeners()

        // 4. LiveData 관찰 및 UI 업데이트
        recommendViewModel.recommendedNewsList.observe(this) { recommendedList ->
            recommendNewsAdapter.updateData(recommendedList)
        }
    }

    private fun setupRecyclerView() {
        recommendNewsAdapter = RecommendNewsAdapter(mutableListOf(), newsViewModel)
        binding.rvRecommendedNews.adapter = recommendNewsAdapter
        binding.rvRecommendedNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun setupEventListeners() {
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_recommend -> true
                R.id.navigation_scrap -> {
                    startActivity(Intent(this, ScrapActivity::class.java))
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}