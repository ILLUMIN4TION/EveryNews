package com.example.everynewsapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // ★★★ Activity ViewModel 공유를 위해 사용 ★★★
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.everynewsapp.R
import com.example.everynewsapp.databinding.FragmentHomeBinding
import com.example.everynewsapp.news.viewModel.NewsViewModel
import com.google.android.material.chip.Chip

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ★★★ Activity ViewModel 공유 ★★★
    private val newsViewModel: NewsViewModel by activityViewModels()

    private lateinit var defaultNewsAdapter: NewsAdapter
    private lateinit var trendingNewsAdapter: TrendingNewsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 기존 activity_main.xml의 콘텐츠를 fragment_home.xml로 옮겨서 사용합니다.
        // 임시로 activity_main.xml의 바인딩 클래스를 사용하여 테스트합니다.
        // 실제로는 fragment_home.xml을 만들어야 합니다.
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupEventListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // 최신 뉴스 (무한 스크롤 적용)
        defaultNewsAdapter = NewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvDefaultNews.adapter = defaultNewsAdapter
        val defaultLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvDefaultNews.layoutManager = defaultLayoutManager

        // ★★★ NestedScrollView 스크롤 끝 감지 로직 ★★★
        binding.nestedScrollView.setOnScrollChangeListener { v: androidx.core.widget.NestedScrollView, _, scrollY, _, oldScrollY ->
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

        // 로딩 상태 관찰 및 프로그레스바 제어
        newsViewModel.isLoadInProgress.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupEventListeners() {
        // Chip Group 이벤트 처리 (MainActivity 로직 그대로 이동)
        val chips = mapOf(
            R.id.chipPolitics to "정치",
            R.id.chipTechnology to "IT",
            R.id.chipSports to "스포츠",
            R.id.chipEntertainment to "연예"
        )

        chips.forEach { (chipId, query) ->
            binding.chipGroupCategory.findViewById<Chip>(chipId)?.setOnClickListener {
                Toast.makeText(context, "$query 카테고리 선택", Toast.LENGTH_SHORT).show()
                newsViewModel.searchNewsByCategory(query)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
