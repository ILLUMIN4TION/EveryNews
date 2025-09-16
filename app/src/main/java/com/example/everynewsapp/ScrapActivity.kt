package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.databinding.ActivityScrapBinding
import com.example.everynewsapp.news.repository.NewsRepository
import com.example.everynewsapp.news.database.AppDatabase
import com.example.everynewsapp.news.viewModel.NewsDetailViewModelFactory
import com.example.everynewsapp.news.viewModel.ViewModels
import com.example.everynewsapp.ui.ScrappedNewsAdapter

class ScrapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScrapBinding
    private lateinit var scrappedNewsViewModel: ViewModels.ScrappedNewsViewModel
    private lateinit var scrappedNewsAdapter: ScrappedNewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScrapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 현재 탭을 스크랩으로 설정
        binding.bottomNavigationView.selectedItemId = R.id.navigation_scrap

        // ★★★ 뷰모델 초기화 및 데이터 로드 로직 추가 ★★★
        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)
        val viewModelFactory = NewsDetailViewModelFactory(newsRepository)
        scrappedNewsViewModel = ViewModelProvider(this, viewModelFactory).get(ViewModels.ScrappedNewsViewModel::class.java)

        setupRecyclerView()

        // LiveData를 관찰하여 UI 업데이트
        scrappedNewsViewModel.scrappedNews.observe(this) { scrappedList ->
            scrappedNewsAdapter.updateData(scrappedList)
        }

        // ★★★ 기존 바텀 네비게이션 뷰 리스너는 그대로 유지 ★★★
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

    private fun setupRecyclerView() {
        scrappedNewsAdapter = ScrappedNewsAdapter()
        binding.rvScrappedNews.adapter = scrappedNewsAdapter
        binding.rvScrappedNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }
}