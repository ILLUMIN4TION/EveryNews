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

    // Activity ViewModel 공유
    private val newsViewModel: NewsViewModel by activityViewModels()

    private lateinit var defaultNewsAdapter: NewsAdapter
    private lateinit var trendingNewsAdapter: TrendingNewsAdapter

    // 칩 ID와 쿼리 문자열 리소스 ID 매핑
    private val chipQueries = mapOf(
        R.id.chipPolitics to R.string.category_politics,
        R.id.chipTechnology to R.string.category_technology,
        R.id.chipSports to R.string.category_sports,
        R.id.chipEntertainment to R.string.category_entertainment,
        R.id.chipEconomy to R.string.category_economy,
        R.id.chipWorld to R.string.category_world,
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
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // strings.xml 리소스를 사용하도록 수정
            newsViewModel.searchNewsByCategory(getString(R.string.section_latest_news))
            newsViewModel.fetchTrendingNews(getString(R.string.section_trending_news))
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

        // FAB 버튼 클릭 리스너 설정
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
    }

    private fun setupEventListeners() {
        // ChipGroup 리스너 대신 개별 칩에 리스너를 설정합니다. (더 안정적인 방식)

        // 칩 ID와 해당 칩 뷰를 매핑
        val chipViews = mapOf(
            R.id.chipPolitics to binding.chipPolitics,
            R.id.chipTechnology to binding.chipTechnology,
            R.id.chipSports to binding.chipSports,
            R.id.chipEntertainment to binding.chipEntertainment,
            R.id.chipEconomy to binding.chipEconomy,
            R.id.chipWorld to binding.chipWorld,
        )

        chipViews.forEach { (id, chip) ->
            chip?.setOnClickListener {
                // 1. 해당 칩을 선택 상태로 만들고, 다른 모든 칩을 선택 해제 (단일 선택 강제)
                binding.chipGroupCategory.check(id)

                // 2. 쿼리 추출
                val queryResourceId = chipQueries[id]
                val query = if (queryResourceId != null) {
                    getString(queryResourceId)
                } else {
                    getString(R.string.section_latest_news)
                }

                Log.d("ChipEvent", "Chip clicked. New Query: $query")

                // 3. 뷰모델 함수 호출
                newsViewModel.searchNewsByCategory(query)
                Toast.makeText(context, "$query 선택", Toast.LENGTH_SHORT).show()
            }
        }

        // 초기 로드 후에도 칩 그룹의 상태를 설정하여 시각적 피드백 제공 (선택된 상태 유지)
        if (binding.chipGroupCategory.checkedChipIds.isEmpty()) {
            binding.chipPolitics.isChecked = true // 예시: Politics를 기본 선택 상태로 만듭니다.
        }

        // 기존 ChipGroup 리스너는 제거합니다. (이제 개별 칩 리스너가 모든 것을 처리합니다)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
