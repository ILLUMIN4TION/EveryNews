package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var trendingNewsAdapter: TrendingNewsAdapter
    private lateinit var newsViewModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)
        val viewModelFactory = NewsViewModelFactory(newsRepository, application)
        newsViewModel = ViewModelProvider(this, viewModelFactory).get(NewsViewModel::class.java)

        setupRecyclerView()
        setupEventListeners()
        observeViewModel()
    }


    private fun setupRecyclerView() {
        //후행람다 매개변수 , 각 아이템 별로 스크랩 버튼 누르면 람다 문 실행
        newsAdapter = NewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvDefaultNews.adapter = newsAdapter
        binding.rvDefaultNews.layoutManager = LinearLayoutManager(this)

        trendingNewsAdapter = TrendingNewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvTrendingNews.adapter = trendingNewsAdapter
        binding.rvTrendingNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        binding.rvDefaultNews.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()

                    if (lastVisibleItemPosition >= totalItemCount - 3) {
                        newsViewModel.loadMoreDefaultNews()
                    }
                }
            }
        })
    }

    private fun observeViewModel() {
        newsViewModel.defaultNewsList.observe(this) { newsItems ->
            newsAdapter.updateData(newsItems)
        }

        newsViewModel.trendingNewsList.observe(this) { newsItems ->
            trendingNewsAdapter.updateData(newsItems)
        }
    }

    private fun setupEventListeners() {
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

        val chips = mapOf(
            R.id.chipPolitics to "정치",
            R.id.chipTechnology to "IT",
            R.id.chipSports to "스포츠",
            R.id.chipEntertainment to "연예"
        )

        chips.forEach { (chipId, query) ->
            binding.chipGroupCategory.findViewById<Chip>(chipId)?.setOnClickListener {
                Toast.makeText(this, "$query 카테고리 선택", Toast.LENGTH_SHORT).show()
                newsViewModel.searchNewsByCategory(query)
            }
        }
    }
}