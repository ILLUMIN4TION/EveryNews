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

        // ★★★ SwipeRefreshLayout 초기 설정 ★★★
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        // SwipeRefreshLayout 새로고침 리스너
        binding.swipeRefreshLayout.setOnRefreshListener {
            // 칩이나 카테고리 상태에 관계없이 초기 쿼리("최신")로 새로고침 요청
            newsViewModel.searchNewsByCategory("최신")
            // 인기 뉴스도 새로고침
            newsViewModel.fetchTrendingNews("인기 뉴스")
            // isRefreshing은 isLoadInProgress.observe에서 false로 자동 설정됨
        }
    }

    private fun setupRecyclerViews() {
        // 최신 뉴스 (무한 스크롤 적용)
        defaultNewsAdapter = NewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvDefaultNews.adapter = defaultNewsAdapter
        val defaultLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvDefaultNews.layoutManager = defaultLayoutManager

        // NestedScrollView 스크롤 끝 감지 로직 (무한 스크롤)
        binding.nestedScrollView.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            // 스크롤 끝 감지 (Load More)
            if (scrollY == (v.getChildAt(0).measuredHeight - v.measuredHeight) && scrollY > oldScrollY) {
                Log.d("HomeFragment", "NestedScrollView End Reached. Loading More News...")
                newsViewModel.loadMoreDefaultNews()
            }

            // FAB 버튼 가시성 제어
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

        // 인기 뉴스 (한 번만 로드)
        trendingNewsAdapter = TrendingNewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvTrendingNews.adapter = trendingNewsAdapter
        binding.rvTrendingNews.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun observeViewModel() {
        // ViewModel의 LiveData를 관찰하여 UI 업데이트
        newsViewModel.defaultNewsList.observe(viewLifecycleOwner) { newsList ->
            defaultNewsAdapter.updateData(newsList)
        }

        newsViewModel.trendingNewsList.observe(viewLifecycleOwner) { newsList ->
            trendingNewsAdapter.updateData(newsList)
        }

        newsViewModel.loadMoreEvent.observe(viewLifecycleOwner) { newItems ->
            defaultNewsAdapter.addData(newItems)
        }

        // ★★★ 로딩 상태 관찰 및 ProgressBar, SwipeRefreshLayout 제어 ★★★
        newsViewModel.isLoadInProgress.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // 로딩이 완료되었을 때만 새로고침 애니메이션을 멈춥니다.
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupEventListeners() {
        // Chip Group 이벤트 처리
        binding.chipGroupCategory.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                newsViewModel.searchNewsByCategory("최신")
            } else {
                val selectedChipId = checkedIds.first()
                val selectedChip = view?.findViewById<Chip>(selectedChipId)
                val query = selectedChip?.text.toString() ?: "최신"
                newsViewModel.searchNewsByCategory(query)
                Toast.makeText(context, "$query 카테고리 선택", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
