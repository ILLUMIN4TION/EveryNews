package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.databinding.ActivityScrapBinding
import com.example.everynewsapp.news.repository.NewsRepository
import com.example.everynewsapp.news.database.AppDatabase
import com.example.everynewsapp.news.viewModel.ScrappedNewsViewModel
import com.example.everynewsapp.news.viewModel.ScrappedNewsViewModelFactory
import com.example.everynewsapp.ui.ScrappedNewsAdapter

class ScrapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScrapBinding
    private lateinit var scrappedNewsViewModel: ScrappedNewsViewModel // ScrappedNewsViewModel로 변경
    private lateinit var scrappedNewsAdapter: ScrappedNewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityScrapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.selectedItemId = R.id.navigation_scrap

        // 1. 데이터베이스, DAO, Repository 초기화
        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)

        // 2. ViewModel Factory 초기화
        val viewModelFactory = ScrappedNewsViewModelFactory(newsRepository)

        // 3. ViewModel 초기화 (팩토리를 사용)
        scrappedNewsViewModel = ViewModelProvider(this, viewModelFactory).get(ScrappedNewsViewModel::class.java)

        // 4. RecyclerView 및 어댑터 설정
        setupRecyclerView()

        // 5. LiveData를 관찰하여 UI 업데이트
        scrappedNewsViewModel.scrappedNews.observe(this) { scrappedList ->
            scrappedNewsAdapter.updateData(scrappedList)
        }

        // 6. BottomNavigationView 리스너 설정
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