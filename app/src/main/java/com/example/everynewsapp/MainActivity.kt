package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    private var currentNewsPage = 1 // 최신 뉴스 현재 페이지
    private val newsDisplayCount = 10 // 한 번에 가져올 최신 뉴스 개수
    private var isLoading = false // 뉴스 로딩 중복 방지 플래그

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

        fetchDefaultNews("최신 뉴스", newsDisplayCount, currentNewsPage )
        fetchTrendingNews("인기 뉴스",15)

        // 6. BottomNavigationView 리스너 설정
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    // 현재 액티비티가 MainActivity이므로, 다시 시작할 필요가 없을 수 있습니다.
                    // 혹은 특정 데이터를 새로고침하는 등의 동작을 할 수 있습니다.
                    // startActivity(Intent(this, MainActivity::class.java)) // 필요하다면 유지
                    true
                }
                R.id.navigation_recommend -> {
                    startActivity(Intent(this, RecommendActivity::class.java))
                    true
                }
                R.id.navigation_scrap -> { // !!! 수정된 부분 !!!
                    startActivity(Intent(this, ScrapActivity::class.java)) // ScrapActivity로 이동
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

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(viewModel = newsDetailViewModel)
        binding.rvDefaultNews.adapter = newsAdapter
        // rvDefaultNews의 LayoutManager는 XML에서 horizontal로 설정되어 있다면,
        // 무한 스크롤 로직도 수평 스크롤 기준으로 수정 필요.
        // 여기서는 수직 스크롤을 가정하고 LinearLayoutManager 기본값 사용.
        // 만약 XML에서 horizontal이라면, 아래 리스너 로직도 dx를 기준으로 판단해야 함.
        binding.rvDefaultNews.layoutManager = LinearLayoutManager(this)

        binding.rvDefaultNews.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()

                // 조건: 아래로 스크롤 중이고(dy > 0), 로딩 중이 아니며,
                // 마지막으로 보이는 아이템이 거의 끝에 도달했고 (예: 3개 아이템 남음),
                // 현재 아이템 수가 최소 한 페이지 분량 이상일 때
                if (dy > 0 && !isLoading && totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 3 && totalItemCount >= newsDisplayCount) {
                    currentNewsPage++
                    Log.d("MainActivity", "Requesting next page for default news. Page: $currentNewsPage. Total items: $totalItemCount, LastVisible: $lastVisibleItemPosition")
                    fetchDefaultNews("최신 뉴스", newsDisplayCount, currentNewsPage, true)
                }
            }
        })

        trendingNewsAdapter = TrendingNewsAdapter(viewModel = newsDetailViewModel)
        binding.rvTrendingNews.adapter = trendingNewsAdapter
        binding.rvTrendingNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }


    private fun fetchDefaultNews(query: String, display: Int, startPage: Int, isLoadMore: Boolean = false) {
        if (isLoading && isLoadMore) return // 추가 로드 시에만 중복 방지 (초기 로드는 진행)
        isLoading = true
        val startItemPosition = (startPage - 1) * display + 1

        lifecycleScope.launch {
            Log.d("MainActivity", "Fetching default news. Query: $query, Display: $display, Start Page: $startPage, API Start Pos: $startItemPosition, IsLoadMore: $isLoadMore")
            val fetchedItems = NaverNewsApi.fetchNews(query, display, startItemPosition)
            if (fetchedItems != null && fetchedItems.isNotEmpty()) { // 아이템이 실제로 있을 때만 처리
                if (isLoadMore) {
                    newsAdapter.addData(fetchedItems)
                } else {
                    newsAdapter.updateData(fetchedItems)
                }
                Log.d("MainActivity", "Default news fetched/added: ${fetchedItems.size} items. IsLoadMore: $isLoadMore. Total after: ${newsAdapter.itemCount}")
            } else if (fetchedItems == null) { // API 호출 실패
                Log.e("MainActivity", "기본 뉴스 불러오기 실패 (API 오류)")
                if (isLoadMore) currentNewsPage--
            } else { // fetchedItems.isEmpty() - 더 이상 아이템이 없음
                Log.d("MainActivity", "기본 뉴스: 더 이상 불러올 아이템 없음. Query: $query, Page: $startPage")
                if (isLoadMore) currentNewsPage-- // 페이지는 증가시키지 않음
            }
            isLoading = false
        }
    }

    private fun fetchTrendingNews(query: String, display: Int) {
        lifecycleScope.launch {
            val fetchedItems = NaverNewsApi.fetchNews(query, display = display, start = 1)
            if (fetchedItems != null) {
                trendingNewsAdapter.updateData(fetchedItems)
            } else {
                Log.e("MainActivity", "트렌딩 뉴스 불러오기 실패 또는 결과 없음")
            }
        }
    }

    private fun setupEventListeners() {
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            // ... (기존 BottomNavigationView 리스너 코드) ...
            true // return true 추가
        }

        val chips = mapOf(
            R.id.chipPolitics to "정치",
            R.id.chipTechnology to "IT", // "Technology" 대신 "IT" 등 API가 이해하는 검색어로
            R.id.chipSports to "스포츠",
            R.id.chipEntertainment to "연예"
            // 필요에 따라 다른 칩들도 추가
        )

        chips.forEach { (chipId, query) ->
            binding.chipGroupCategory.findViewById<Chip>(chipId)?.setOnClickListener {
                Toast.makeText(this, "$query 카테고리 선택", Toast.LENGTH_SHORT).show()
                currentNewsPage = 1
                newsAdapter.clearData() // 또는 updateData(emptyList())
                fetchDefaultNews(query, newsDisplayCount, currentNewsPage)
                // 트렌딩 뉴스도 카테고리에 맞게 변경할지 여부
                // fetchTrendingNews(query, 15)
            }
        }
    }
}