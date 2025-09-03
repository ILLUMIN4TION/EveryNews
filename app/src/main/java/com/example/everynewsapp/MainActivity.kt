package com.example.everynewsapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.databinding.ActivityMainBinding // ViewBinding import
import com.example.everynewsapp.news.network.NaverNewsApi
import com.example.everynewsapp.news.ui.NewsAdapter
import com.example.everynewsapp.news.ui.TrendingNewsAdapter
import com.google.android.material.chip.Chip // Chip import 추가

class MainActivity : AppCompatActivity() {

    // ViewBinding으로 뷰를 안전하게 제어합니다.
    private lateinit var binding: ActivityMainBinding
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var trendingNewsAdapter: TrendingNewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ViewBinding을 사용하여 레이아웃을 설정합니다.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. RecyclerView 초기화 (LayoutManager 설정 추가)
        setupRecyclerView()

        // 2. 기존 뉴스 불러오기 기능 호출
        fetchNews("인기 뉴스")


        // 3. 추가된 UI 이벤트 리스너 설정
        setupEventListeners()
    }

    /**
     * RecyclerView를 초기 설정하는 함수
     */
    private fun setupRecyclerView() {

        newsAdapter = NewsAdapter(emptyList())
        binding.rvDefaultNews.adapter = newsAdapter
        binding.rvDefaultNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        trendingNewsAdapter = TrendingNewsAdapter(emptyList())
        binding.rvTrendingNews.adapter = trendingNewsAdapter
        binding.rvTrendingNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false)

//        TrendingNewsAdapter = TrendingNewsAdapter(emptyList()) // 초기엔 빈 리스트로 어댑터 생성
//        binding.rvTrendingNews.adapter = TrendingNewsAdapter
//        // RecyclerView가 아이템들을 어떻게 보여줄지(가로 방향으로) 결정하는 LayoutManager 설정
//        binding.rvTrendingNews.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    /**
     * 하단 네비게이션, 칩 등 UI 요소들의 이벤트 리스너를 설정하는 함수
     */
    private fun setupEventListeners() {
        // BottomNavigationView 메뉴 선택 이벤트 처리
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    Toast.makeText(this, "홈 선택", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_search -> {
                    Toast.makeText(this, "AI 추천 선택", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_scrap -> {
                    Toast.makeText(this, "스크랩 선택", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_settings -> {
                    Toast.makeText(this, "설정 선택", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        // 카테고리 Chip 클릭 이벤트 처리 (예시: Politics 칩)
        binding.chipGroupCategory.findViewById<Chip>(R.id.chipPolitics).setOnClickListener {
            Toast.makeText(this, "정치 카테고리 선택", Toast.LENGTH_SHORT).show()
            // 선택된 카테고리로 뉴스를 새로 불러올 수 있습니다.
            // fetchNews("정치")
        }
    }

    /**
     * 네이버 뉴스 API를 통해 뉴스를 가져오는 기존 함수 (수정됨)
     */
    private fun fetchNews(query: String) {
        NaverNewsApi.fetchNews(query) { newsResponse ->
            // 응답 결과가 null이 아닐 때만 UI 업데이트
            newsResponse?.let {
                // runOnUiThread를 통해 메인 스레드에서 UI 작업을 수행
                runOnUiThread {
                    // 어댑터에 새로운 뉴스 리스트를 전달하고, RecyclerView에 다시 연결
                    newsAdapter = NewsAdapter(it)
                    binding.rvDefaultNews.adapter = newsAdapter


                    trendingNewsAdapter = TrendingNewsAdapter(it)
                    binding.rvTrendingNews.adapter = trendingNewsAdapter
                }
            } ?: run {
                Log.e("MainActivity", "뉴스 불러오기 실패 또는 결과 없음")
            }
        }
    }
}
