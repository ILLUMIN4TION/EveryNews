package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.everynewsapp.databinding.ActivityMainBinding
import com.example.everynewsapp.news.database.AppDatabase
import com.example.everynewsapp.news.repository.NewsRepository
import com.example.everynewsapp.news.viewModel.NewsViewModel
import com.example.everynewsapp.news.viewModel.NewsViewModelFactory
import com.example.everynewsapp.ui.NewsAdapter
import com.example.everynewsapp.ui.TrendingNewsAdapter
import com.google.android.material.chip.Chip
import androidx.core.widget.NestedScrollView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var newsViewModel: NewsViewModel
    private lateinit var defaultNewsAdapter: NewsAdapter
    private lateinit var trendingNewsAdapter: TrendingNewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.selectedItemId = R.id.navigation_home

        // 1. ViewModel 초기화
        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)
        val viewModelFactory = NewsViewModelFactory(newsRepository, application)
        newsViewModel = ViewModelProvider(this, viewModelFactory).get(NewsViewModel::class.java)

        setupRecyclerViews()
        setupEventListeners()

        // 2. ViewModel의 LiveData를 관찰하여 UI 업데이트
        newsViewModel.defaultNewsList.observe(this) { newsList ->
            Log.d("MainActivity", "Default News Loaded: ${newsList.size} items")
            defaultNewsAdapter.updateData(newsList)
            // 데이터 로드 후 뷰 갱신 로직 추가
            if (newsList.isNotEmpty()) {
                binding.rvDefaultNews.post {
                    binding.rvDefaultNews.requestLayout()
                }
            }
        }

        newsViewModel.trendingNewsList.observe(this) { newsList ->
            trendingNewsAdapter.updateData(newsList)
        }

        // 3. loadMoreEvent 옵저버 추가 (addData 사용)
        newsViewModel.loadMoreEvent.observe(this) { newItems ->
            defaultNewsAdapter.addData(newItems)
            Log.d("MainActivity", "Load More Success: ${newItems.size} items added")
            // 데이터 추가 후 뷰 갱신 로직 추가
            binding.rvDefaultNews.post {
                binding.rvDefaultNews.requestLayout()
            }
        }

        // ★★★ 4. 로딩 상태 관찰 및 프로그레스바 제어 ★★★
        newsViewModel.isLoadInProgress.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupRecyclerViews() {
        // 최신 뉴스 (무한 스크롤 적용)
        defaultNewsAdapter = NewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvDefaultNews.adapter = defaultNewsAdapter
        val defaultLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvDefaultNews.layoutManager = defaultLayoutManager

        // ★★★ NestedScrollView의 스크롤 끝 감지 로직으로 대체 ★★★
        binding.nestedScrollView.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            // 스크롤 끝 감지 (Load More)
            if (scrollY == (v.getChildAt(0).measuredHeight - v.measuredHeight) && scrollY > oldScrollY) {
                Log.d("ScrollCheck", "NestedScrollView End Reached. Loading More News...")
                newsViewModel.loadMoreDefaultNews()
            }

            // ★★★ FAB 버튼 가시성 제어 ★★★
            if (scrollY > 500) { // 500px 이상 스크롤 시 버튼 표시
                binding.fabScrollToTop.visibility = View.VISIBLE
            } else {
                binding.fabScrollToTop.visibility = View.GONE
            }
        }

        // ★★★ FAB 버튼 클릭 리스너 설정 ★★★
        binding.fabScrollToTop.setOnClickListener {
            binding.nestedScrollView.smoothScrollTo(0, 0) // 최상단으로 부드럽게 스크롤
        }


        // 인기 뉴스 (한 번만 로드)
        trendingNewsAdapter = TrendingNewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvTrendingNews.adapter = trendingNewsAdapter
        binding.rvTrendingNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupEventListeners() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                newsViewModel.searchNewsByCategory("최신")
            } else {
                val selectedChipId = checkedIds.first()
                val selectedChip = findViewById<com.google.android.material.chip.Chip>(selectedChipId)
                val query = selectedChip.text.toString()
                newsViewModel.searchNewsByCategory(query)
                Toast.makeText(this, "$query 선택", Toast.LENGTH_SHORT).show()
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_recommend -> {
                    startActivity(Intent(this, RecommendActivity::class.java))
                    true
                }
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
