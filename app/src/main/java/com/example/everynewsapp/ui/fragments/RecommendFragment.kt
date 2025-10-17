package com.example.everynewsapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.news.viewModel.RecommendNewsViewModel
import com.example.everynewsapp.news.viewModel.RecommendNewsViewModelFactory
import com.example.everynewsapp.databinding.FragmentRecommendScrapBinding
import com.example.everynewsapp.news.database.AppDatabase
import com.example.everynewsapp.news.repository.NewsRepository
import com.example.everynewsapp.news.viewModel.NewsViewModel

class RecommendFragment : Fragment() {

    private var _binding: FragmentRecommendScrapBinding? = null
    private val binding get() = _binding!!

    // 메인 뉴스 뷰모델 (스크랩 토글을 위해 공유)
    private val newsViewModel: NewsViewModel by activityViewModels()

    // 추천 로직을 위한 별도의 뷰모델 (이 프래그먼트의 생명주기 따름)
    private lateinit var recommendViewModel: RecommendNewsViewModel
    private lateinit var recommendNewsAdapter: RecommendNewsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendScrapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupEventListeners()
        observeViewModel()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)

        val factory = RecommendNewsViewModelFactory(newsRepository)
        // Fragment의 생명주기를 따르는 ViewModel 초기화
        recommendViewModel = ViewModelProvider(this, factory).get(RecommendNewsViewModel::class.java)
    }

    private fun setupRecyclerView() {
        // NewsViewModel의 toggleScrap 함수를 사용하도록 설정
        recommendNewsAdapter = RecommendNewsAdapter(mutableListOf(), newsViewModel)
        binding.rvRecommendedNews.adapter = recommendNewsAdapter
        binding.rvRecommendedNews.layoutManager = LinearLayoutManager(context)
    }

    private fun setupEventListeners() {
        // 새로 추천받기 버튼 클릭 시 뷰모델의 새로고침 함수 호출
        binding.btnRefreshRecommend.setOnClickListener {
            recommendViewModel.fetchRecommendedNews()
        }
    }

    private fun observeViewModel() {
        recommendViewModel.recommendedNewsList.observe(viewLifecycleOwner) { newsList ->
            recommendNewsAdapter.updateData(newsList)
            // 로딩 완료 후 프로그레스바 숨기기
            binding.pbRecommendLoading.visibility = View.GONE
        }

        // 초기 로딩 상태 처리 (RecommendViewModel에 로딩 상태 LiveData가 있다고 가정)
        // 현재 RecommendViewModel에는 없지만, 나중에 추가된다고 가정하고 ProgressBar를 미리 준비
        // if (recommendViewModel.isLoading.value == true) {
        //     binding.pbRecommendLoading.visibility = View.VISIBLE
        // }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
