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
            newsViewModel.searchNewsByCategory(getString(R.string.section_latest_news))
            Log.d("HomeFragment", "Initial Query: Latest News")

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

        // ★★★ START: 이 코드 블록을 삭제하세요 (151 ~ 157라인) ★★★
        // newsViewModel.clearChipSelectionEvent.observe(viewLifecycleOwner) { shouldClear ->
        //     if (shouldClear) {
        //         Log.d("HomeFragment", "Clearing chips due to search")
        //         binding.chipGroupCategory.clearCheck()
        //     }
        // }
        // ★★★ END: 삭제할 코드 블록 ★★★
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

            newsViewModel.searchNewsByCategory(query)
            Toast.makeText(context, "$query 선택", Toast.LENGTH_SHORT).show()
            Log.d("ChipEvent", "Query Executed: $query (ID: $clickedChipId)")

            // ★★★ START: 칩 클릭 시 SearchView를 닫는 이벤트 호출 (추가) ★★★
            // newsViewModel.collapseSearchViewEvent.postValue(true) // <- 이 줄도 NewsViewModel에서 제거되었으므로 삭제합니다.
            // ★★★ END ★★★
        }

        binding.chipPolitics.setOnClickListener(chipClickListener)
        binding.chipTechnology.setOnClickListener(chipClickListener)
        binding.chipSports.setOnClickListener(chipClickListener)
        binding.chipEntertainment.setOnClickListener(chipClickListener)
        binding.chipEconomy.setOnClickListener(chipClickListener)
        binding.chipWorld.setOnClickListener(chipClickListener)

        // ★★★ ChipGroup 리스너 제거 ★★★
        // binding.chipGroupCategory.setOnCheckedChangeListener { group, checkedId -> ... }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}