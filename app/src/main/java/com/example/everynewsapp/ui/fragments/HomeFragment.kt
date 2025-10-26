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

    // ★★★ 변경 ★★★
    // '정치' 칩의 초기(프로그래매틱) 클릭으로 인한 중복 쿼리 방지 플래그
    // 이 플래그는 더 이상 ChipGroup 리스너에서 사용되지 않지만,
    // onViewCreated의 초기화 로직을 명확히 하기 위해 남겨둘 수 있습니다. (사실상 필요 없어짐)
    // private var skipNextQuery = false // -> setupEventListeners에서 이 로직을 제거함

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
        setupEventListeners() // ★★★ 가장 중요 ★★★
        observeViewModel()
        setupSwipeRefresh()

        // ★★★ 변경: 사용자의 요구사항 반영 ★★★
        if (savedInstanceState == null) {
            // 1. 앱 시작 시 무조건 '최신뉴스'를 조회합니다. (요구사항 1)
            newsViewModel.searchNewsByCategory(getString(R.string.section_latest_news))
            Log.d("HomeFragment", "Initial Query: Latest News")

            // 2. '정치' 칩을 시각적으로만 선택합니다. (리스너가 분리되어 쿼리 실행 안 됨)
            binding.chipGroupCategory.check(R.id.chipPolitics)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            val currentQuery = getCurrentSelectedQuery()
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
        // (기존 코드와 동일)
        defaultNewsAdapter = NewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvDefaultNews.adapter = defaultNewsAdapter
        val defaultLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvDefaultNews.layoutManager = defaultLayoutManager

        binding.nestedScrollView.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            if (scrollY == (v.getChildAt(0).measuredHeight - v.measuredHeight) && scrollY > oldScrollY) {
                Log.d("HomeFragment", "NestedScrollView End Reached. Loading More News...")
                newsViewModel.loadMoreDefaultNews()
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
        // (기존 코드와 동일, viewFalser 오타 수정됨)
        newsViewModel.defaultNewsList.observe(viewLifecycleOwner) { newsList ->
            defaultNewsAdapter.updateData(newsList)
        }

        newsViewModel.trendingNewsList.observe(viewLifecycleOwner) { newsList ->
            trendingNewsAdapter.updateData(newsList)
        }

        newsViewModel.loadMoreEvent.observe(viewLifecycleOwner) { newItems ->
            defaultNewsAdapter.addData(newItems)
        }

        newsViewModel.isLoadInProgress.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        // 2. ★★★ MainActivity의 검색에 반응하는 옵저버 (추가) ★★★
        newsViewModel.clearChipSelectionEvent.observe(viewLifecycleOwner) { shouldClear ->
            if (shouldClear) {
                Log.d("HomeFragment", "Clearing chips due to search")
                binding.chipGroupCategory.clearCheck()
            }
        }
    }

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ★★★ 변경: 리스너 설정 로직 전체 변경 ★★★
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    private fun setupEventListeners() {


        val chipClickListener = View.OnClickListener { view ->
            // 클릭된 칩의 ID를 가져옵니다 (예: R.id.chipPolitics)
            val clickedChipId = view.id

            // 1. ChipGroup이 클릭된 칩을 시각적으로 선택하도록 강제합니다.
            binding.chipGroupCategory.check(clickedChipId)

            // 2. 맵(chipQueries)을 사용해 ID에 해당하는 쿼리(R.string.query_politics)를 찾습니다.
            val queryResourceId = chipQueries[clickedChipId]

            val query = if (queryResourceId != null) {
                getString(queryResourceId) // "정치", "기술" 등 실제 쿼리 문자열
            } else {
                // 맵에 없는 칩이 눌린 경우 (안전 장치)
                getString(R.string.section_latest_news)
            }

            // 3. 뷰모델로 쿼리를 실행합니다.
            newsViewModel.searchNewsByCategory(query)
            Toast.makeText(context, "$query 선택", Toast.LENGTH_SHORT).show()
            Log.d("ChipEvent", "Query Executed: $query (ID: $clickedChipId)")
        }

        // 바인딩을 통해 각 칩에 위에서 만든 리스너를 할당합니다.
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