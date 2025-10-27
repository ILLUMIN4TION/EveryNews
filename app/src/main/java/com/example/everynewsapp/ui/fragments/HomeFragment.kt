package com.example.everynewsapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.R
import com.example.everynewsapp.databinding.FragmentHomeBinding
import com.example.everynewsapp.news.viewModel.NewsViewModel
import com.google.android.material.tabs.TabLayout // ★★★ TabLayout 임포트 ★★★

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Activity와 동일한 ViewModel 인스턴스 사용
    private val newsViewModel: NewsViewModel by activityViewModels()

    private lateinit var defaultNewsAdapter: NewsAdapter
    private lateinit var trendingNewsAdapter: TrendingNewsAdapter

    // ★★★ 수정: 카테고리 리스트 ★★★
    // 탭에 표시할 문자열 리스트를 만듭니다.
    private lateinit var categories: List<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // ★★★ 카테고리 리스트 초기화 (getString을 사용하기 위해) ★★★
        categories = listOf(
            getString(R.string.section_latest_news), // "최신 뉴스"를 맨 앞으로
            getString(R.string.query_politics),
            getString(R.string.query_technology),
            getString(R.string.query_sports),
            getString(R.string.query_entertainment),
            getString(R.string.query_economy),
            getString(R.string.query_world)
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupTabLayout() // ★★★ setupEventListeners -> setupTabLayout으로 변경 ★★★
        observeViewModel()
        setupSwipeRefresh()

        // ★★★ (제거) ★★★
        // 초기 로드 로직은 setupTabLayout에서 첫 탭을 선택하는 것으로 변경됨
        // if (savedInstanceState == null) { ... }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            val currentQuery = getCurrentSelectedQuery() // ★★★ 수정된 함수 사용 ★★★
            newsViewModel.searchNewsByCategory(currentQuery)
            newsViewModel.fetchTrendingNews(getString(R.string.section_trending_news))
        }
    }

    // ★★★ 수정: 현재 선택된 '탭'의 쿼리를 가져옴 ★★★
    private fun getCurrentSelectedQuery(): String {
        // 현재 선택된 탭의 인덱스를 가져옵니다.
        val selectedTabIndex = binding.tabLayoutCategory.selectedTabPosition
        // 인덱스가 유효하면 categories 리스트에서 쿼리 문자열을 가져옵니다.
        return if (selectedTabIndex != -1 && selectedTabIndex < categories.size) {
            categories[selectedTabIndex]
        } else {
            // 탭이 선택되지 않은 경우(예: 검색 직후) "최신 뉴스"를 기본값으로
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

    // ★★★ observeViewModel 함수 수정/추가 ★★★
    private fun observeViewModel() {
        // 1. 기존 뉴스 리스트 옵저버 (유지)
        newsViewModel.defaultNewsList.observe(viewLifecycleOwner) { newsList ->
            defaultNewsAdapter.updateData(newsList)
            // (선택 사항: 새 리스트 로드 시 스크롤을 맨 위로)
            binding.nestedScrollView.smoothScrollTo(0, 0)
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

        // 2. ★★★ MainActivity의 검색에 반응하는 옵저버 (수정) ★★★
        newsViewModel.clearChipSelectionEvent.observe(viewLifecycleOwner) { shouldClear ->
            if (shouldClear == true) { // null 체크
                Log.d("HomeFragment", "Clearing tab selection due to search")
                // 탭의 선택을 해제합니다. (리스너를 트리거하지 않습니다)
                binding.tabLayoutCategory.clearOnTabSelectedListeners()
                newsViewModel.clearChipSelectionEvent.postValue(false) // 이벤트 소비
            }
        }
    }

    // ★★★ setupEventListeners -> setupTabLayout 함수로 변경 ★★★
    private fun setupTabLayout() {
        // 1. 카테고리 리스트로 탭을 동적으로 추가
        categories.forEach { categoryName ->
            binding.tabLayoutCategory.addTab(
                binding.tabLayoutCategory.newTab().setText(categoryName)
            )
        }

        // 2. 탭 선택 리스너 설정
        binding.tabLayoutCategory.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val query = it.text.toString()
                    Log.d("HomeFragment", "Tab selected: $query")
                    // ViewModel에 쿼리 요청
                    newsViewModel.searchNewsByCategory(query)
                    // MainActivity의 SearchView를 닫도록 ViewModel에 요청
                    newsViewModel.collapseSearchViewEvent.postValue(true)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // (동작 없음)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // (선택 사항: 탭을 다시 눌렀을 때 새로고침)
                tab?.let {
                    newsViewModel.searchNewsByCategory(it.text.toString())
                    // 스크롤을 맨 위로 올림
                    binding.nestedScrollView.smoothScrollTo(0, 0)
                }
            }
        })

        // 3. (중요) ViewModel의 init{}에서 로드를 제거했으므로,
        //    여기서 첫 번째 탭("최신 뉴스")을 수동으로 선택하여
        //    'onTabSelected' 리스너를 트리거하고 초기 뉴스를 로드합니다.
        if (binding.tabLayoutCategory.selectedTabPosition == -1) {
            binding.tabLayoutCategory.getTabAt(0)?.select()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

