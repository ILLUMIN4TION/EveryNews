package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.databinding.ActivityScrapBinding
import com.example.everynewsapp.news.database.AppDatabase
import com.example.everynewsapp.news.repository.NewsRepository
import com.example.everynewsapp.news.viewModel.NewsViewModel
import com.example.everynewsapp.news.viewModel.NewsViewModelFactory
import com.example.everynewsapp.ui.ScrappedNewsAdapter

class ScrapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScrapBinding
    private lateinit var scrappedNewsAdapter: ScrappedNewsAdapter
    private lateinit var newsViewModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityScrapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.selectedItemId = R.id.navigation_scrap

        // MainActivity와 동일하게 NewsViewModel 초기화
        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)
        val viewModelFactory = NewsViewModelFactory(newsRepository, application)
        newsViewModel = ViewModelProvider(this, viewModelFactory).get(NewsViewModel::class.java)

        setupRecyclerView()
        setupEventListeners()

        // 뷰모델의 LiveData를 관찰하여 UI 업데이트
        newsViewModel.scrappedNewsList.observe(this) { scrappedList ->
            scrappedNewsAdapter.updateData(scrappedList)
        }
    }

    private fun setupRecyclerView() {
        // 어댑터에 초기 빈 리스트와 뷰모델을 전달
        scrappedNewsAdapter = ScrappedNewsAdapter(mutableListOf(), newsViewModel)
        binding.rvScrappedNews.adapter = scrappedNewsAdapter
        binding.rvScrappedNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun setupEventListeners() {
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_recommend -> {
                    startActivity(Intent(this, RecommendActivity::class.java))
                    true
                }
                R.id.navigation_scrap -> true
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}