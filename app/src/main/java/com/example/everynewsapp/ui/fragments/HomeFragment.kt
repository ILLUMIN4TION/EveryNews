package com.example.everynewsapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.everynewsapp.R
import com.example.everynewsapp.databinding.FragmentHomeBinding
import com.example.everynewsapp.news.viewModel.NewsViewModel
import com.google.android.material.chip.Chip
import androidx.core.widget.NestedScrollView

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val newsViewModel: NewsViewModel by activityViewModels()

    private lateinit var defaultNewsAdapter: NewsAdapter
    private lateinit var trendingNewsAdapter: TrendingNewsAdapter

    // 칩 ID와 쿼리 문자열 리소스 ID 매핑 (기존 코드 유지)
    private val chipQueries = mapOf(
        R.id.chipPolitics to R.string.query_politics,
        R.id.chipTechnology to R.string.query_technology,
        R.id.chipSports to R.string.query_sports,
        R.id.chipEntertainment to R.string.query_entertainment,
        R.id.chipEconomy to R.string.query_economy,
        R.id.chipWorld to R.string.query_world,
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupEventListeners()
        observeViewModel()
        setupSwipeRefresh()

        if (savedInstanceState == null) {
            // ★★★ 앱 시작 시 최초 로드 ★★★
            // 최초 로드 시에도 로딩바를 보여주기 위해 칩 클릭 리스너의 로직을 동일하게 적용
            binding.rvDefaultNews.visibility = View.GONE
            binding.progressBarCategory.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE

            newsViewModel.searchNewsByCategory(getString(R.string.section_latest_news))
            Log.d("HomeFragment", "Initial Query: Latest News")

            // ★★★ 수정된 부분: Politics 칩을 강제로 체크하는 라인 삭제 ★★★
            // binding.chipGroupCategory.check(R.id.chipPolitics)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            val currentQuery = getCurrentSelectedQuery()

            // ★★★ 새로고침 시에도 카테고리 로딩바 표시 ★★★
            binding.rvDefaultNews.visibility = View.GONE
            binding.progressBarCategory.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE

            newsViewModel.searchNewsByCategory(currentQuery)
            newsViewModel.fetchTrendingNews(getString(R.string.section_trending_news))
        }
    }

    // 현재 선택된 쿼리를 가져오는 보조 함수 (기존 코드 유지)
    private fun getCurrentSelectedQuery(): String {
        val checkedId = binding.chipGroupCategory.checkedChipId

        if (checkedId == View.NO_ID) {
            return getString(R.string.section_latest_news)
        }
        val queryResourceId = chipQueries[checkedId]

        return if (queryResourceId != null) {
            getString(queryResourceId)
        } else {
            getString(R.string.section_latest_news)
        }
    }


    private fun setupRecyclerViews() {
        defaultNewsAdapter = NewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvDefaultNews.adapter = defaultNewsAdapter
        val defaultLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvDefaultNews.layoutManager = defaultLayoutManager

        binding.nestedScrollView.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            if (scrollY == (v.getChildAt(0).measuredHeight - v.measuredHeight) && scrollY > oldScrollY) {
                Log.d("HomeFragment", "NestedScrollView End Reached. Loading More News...")
                // ★★★ "더 보기" 로드 시에는 카테고리 로딩바가 보이지 않도록 확인 ★★★
                if (binding.progressBarCategory.visibility != View.VISIBLE) {
                    newsViewModel.loadMoreDefaultNews()
                }
            }

            if (scrollY > 500) {
                binding.fabScrollToTop.visibility = View.VISIBLE
            } else {
                binding.fabScrollToTop.visibility = View.GONE
            }
        }

        binding.fabScrollToTop.setOnClickListener {
            binding.nestedScrollView.smoothScrollTo(0, 0)
        }

        trendingNewsAdapter = TrendingNewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvTrendingNews.adapter = trendingNewsAdapter
        binding.rvTrendingNews.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun observeViewModel() {
        newsViewModel.defaultNewsList.observe(viewLifecycleOwner) { newsList ->
            defaultNewsAdapter.updateData(newsList)

            // ★★★ START: 데이터 로드 완료 시, 리스트 표시 및 카테고리 로딩바 숨김 ★★★
            binding.rvDefaultNews.visibility = View.VISIBLE
            binding.progressBarCategory.visibility = View.GONE
            // ★★★ END ★★★
        }

        newsViewModel.trendingNewsList.observe(viewLifecycleOwner) { newsList ->
            trendingNewsAdapter.updateData(newsList)
        }

        newsViewModel.loadMoreEvent.observe(viewLifecycleOwner) { newItems ->
            defaultNewsAdapter.addData(newItems)
        }

        // ★★★ START: "더 보기" 로딩 및 "새로고침" 상태만 제어하도록 수정 ★★★
        newsViewModel.isLoadInProgress.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                // "더 보기" 로딩 중일 때 (즉, 스와이프나 카테고리 로딩이 아닐 때)
                if (!binding.swipeRefreshLayout.isRefreshing && binding.progressBarCategory.visibility != View.VISIBLE) {
                    binding.progressBar.visibility = View.VISIBLE // 하단 "더 보기" 바 표시
                }
            } else {
                // 로딩 완료 시, 스와이프 및 하단 바 모두 숨김
                binding.swipeRefreshLayout.isRefreshing = false
                binding.progressBar.visibility = View.GONE
            }
        }
        // ★★★ END ★★★
    }

    private fun setupEventListeners() {

        val chipClickListener = View.OnClickListener { view ->
            val clickedChipId = view.id
            binding.chipGroupCategory.check(clickedChipId)
            val queryResourceId = chipQueries[clickedChipId]

            val query = if (queryResourceId != null) {
                getString(queryResourceId)
            } else {
                getString(R.string.section_latest_news)
            }

            // ★★★ START: 칩 클릭 시, 리스트 숨기고 카테고리 로딩바 표시 ★★★
            binding.rvDefaultNews.visibility = View.GONE
            binding.progressBarCategory.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE // 하단 "더 보기" 바 숨기기
            // ★★★ END ★★★

            newsViewModel.searchNewsByCategory(query)
            Toast.makeText(context, "$query 선택", Toast.LENGTH_SHORT).show()
            Log.d("ChipEvent", "Query Executed: $query (ID: $clickedChipId)")
        }

        binding.chipPolitics.setOnClickListener(chipClickListener)
        binding.chipTechnology.setOnClickListener(chipClickListener)
        binding.chipSports.setOnClickListener(chipClickListener)
        binding.chipEntertainment.setOnClickListener(chipClickListener)
        binding.chipEconomy.setOnClickListener(chipClickListener)
        binding.chipWorld.setOnClickListener(chipClickListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}