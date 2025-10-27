package com.example.everynewsapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Toast 임포트
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
// import androidx.fragment.app.activityViewModels // activityViewModels 제거
import androidx.fragment.app.viewModels // viewModels 추가
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.R

import com.example.everynewsapp.databinding.FragmentRecommendScrapBinding
import com.example.everynewsapp.news.database.AppDatabase // DB 임포트 추가
import com.example.everynewsapp.news.repository.NewsRepository // Repository 임포트 추가
// import com.example.everynewsapp.news.viewModel.NewsViewModel // NewsViewModel 임포트 제거 (스크랩 필요시 다시 추가)
import com.example.everynewsapp.news.viewModel.RecommendNewsViewModel // RecommendNewsViewModel 임포트 추가
import com.example.everynewsapp.news.viewModel.RecommendNewsViewModelFactory // Factory 임포트 추가

class RecommendFragment : Fragment() {

    private var _binding: FragmentRecommendScrapBinding? = null // ★★★ 바인딩 클래스 확인 ★★★
    private val binding get() = _binding!!

    // ★★★ 수정: RecommendNewsViewModel 사용 및 Factory 주입 ★★★
    private lateinit var newsRepository: NewsRepository
    private val recommendNewsViewModel: RecommendNewsViewModel by viewModels {
        RecommendNewsViewModelFactory(newsRepository)
    }

    // (선택) 스크랩 기능 필요 시 NewsViewModel도 주입
    // private val newsViewModel: NewsViewModel by activityViewModels()

    // 추천 뉴스 어댑터 선언
    private lateinit var recommendNewsAdapter: NewsAdapter // NewsAdapter 재사용 예시

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Repository 초기화 (ViewModel 생성 전에 필요)
        val database = AppDatabase.getDatabase(requireContext().applicationContext)
        val newsDao = database.scrappedNewsDao()
        newsRepository = NewsRepository(newsDao)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendScrapBinding.inflate(inflater, container, false) // ★★★ 바인딩 클래스 확인 ★★★
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupMenu() // 메뉴 (새로고침 아이콘) 설정
        observeViewModel()

        // (ViewModel의 init에서 로드하므로 여기서 호출 불필요)
        // if (savedInstanceState == null) {
        //    recommendNewsViewModel.fetchRecommendedNews()
        // }
    }

    private fun setupRecyclerView() {
        // 추천 뉴스 RecyclerView 설정
        recommendNewsAdapter = NewsAdapter(mutableListOf()) { newsItem ->
            // TODO: 스크랩 기능 구현 (NewsViewModel 필요 시 주입 후 사용)
            // newsViewModel.toggleScrap(newsItem)
            Toast.makeText(requireContext(), "스크랩 기능 연결 필요", Toast.LENGTH_SHORT).show()
        }
        // ★★★ 바인딩 ID 확인 (rvRecommendNews?) ★★★
        binding.rvRecommendedNews.apply {
            adapter = recommendNewsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    // Toolbar 메뉴 설정 (새로고침 아이콘)
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // menu_fragment_recommend.xml 파일 인플레이트
                menuInflater.inflate(R.menu.menu_fragment_recommend, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // 메뉴 아이템 클릭 처리
                return when (menuItem.itemId) {
                    R.id.action_refresh_recommend -> {
                        // ★★★ 수정: RecommendNewsViewModel 함수 호출 ★★★
                        recommendNewsViewModel.fetchRecommendedNews()
                        true // 이벤트 소비됨
                    }
                    else -> false // 처리하지 않음
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED) // 프래그먼트 lifecycle에 맞춰 메뉴 관리
    }


    // ViewModel 관찰 로직
    private fun observeViewModel() {
        // ★★★ 수정: RecommendNewsViewModel의 LiveData 관찰 ★★★
        // 1. 추천 뉴스 리스트 관찰 -> RecyclerView 업데이트 및 빈 상태 처리
        recommendNewsViewModel.recommendedNewsList.observe(viewLifecycleOwner) { newsList ->
            // ★★★ 빈 리스트 확인 로직 추가 ★★★
            if (newsList.isNullOrEmpty()) {
                // 리스트가 비었으면 RecyclerView 숨기고 안내 TextView 표시
                binding.rvRecommendedNews.visibility = View.GONE
                binding.tvEmptyRecommend.visibility = View.VISIBLE // XML에 tvEmptyRecommend ID가 있다고 가정
            } else {
                // 리스트가 있으면 RecyclerView 보여주고 안내 TextView 숨김
                binding.rvRecommendedNews.visibility = View.VISIBLE
                binding.tvEmptyRecommend.visibility = View.GONE // XML에 tvEmptyRecommend ID가 있다고 가정
                // 어댑터에 데이터 업데이트
                recommendNewsAdapter.updateData(newsList)
            }
        }

        // 2. 추천 뉴스 로딩 상태 관찰 -> ProgressBar 가시성 제어
        recommendNewsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // ★★★ 바인딩 ID 확인 (progressBarRecommend?) ★★★
            // 로딩 중일 때는 빈 상태 텍스트를 숨김 (선택적)
            if (isLoading) {
                binding.tvEmptyRecommend.visibility = View.GONE
            }
            binding.pbRecommendLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
}

