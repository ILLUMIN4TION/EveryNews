package com.example.everynewsapp.news.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.everynewsapp.R // R resource import
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.model.ScrappedNewsItem
import com.example.everynewsapp.news.model.toScrappedNewsItem
import com.example.everynewsapp.news.network.NaverNewsApi
import com.example.everynewsapp.news.repository.NewsRepository
import com.example.everynewsapp.ui.LockScreenNewsManager
import kotlinx.coroutines.launch

class NewsViewModel(
    private val newsRepository: NewsRepository,
    application: Application
) : AndroidViewModel(application) {

    // --- HomeFragment가 관찰하는 LiveData ---

    // 기본 뉴스 리스트 (카테고리별 또는 검색별)
    private val _defaultNewsList = MutableLiveData<List<NewsItem>>()
    val defaultNewsList: LiveData<List<NewsItem>> get() = _defaultNewsList

    // 트렌딩 뉴스(인기 뉴스) 리스트
    private val _trendingNewsList = MutableLiveData<List<NewsItem>>()
    val trendingNewsList: LiveData<List<NewsItem>> get() = _trendingNewsList

    // 더 보기 로드 이벤트
    private val _loadMoreEvent = MutableLiveData<List<NewsItem>>()
    val loadMoreEvent: LiveData<List<NewsItem>> get() = _loadMoreEvent

    // 로딩 중 상태 (ProgressBar 및 SwipeRefresh)
    private val _isLoadInProgress = MutableLiveData(false)
    val isLoadInProgress: LiveData<Boolean> get() = _isLoadInProgress

    // 스크랩 리스트
    private val _scrappedNewsList = newsRepository.getAllScrappedNews().asLiveData()
    val scrappedNewsList: LiveData<List<ScrappedNewsItem>> get() = _scrappedNewsList


    // ★★★ MainActivity <-> HomeFragment 통신용 LiveData ★★★
    // 검색 실행 시 -> HomeFragment의 칩 선택을 해제하기 위한 이벤트
    val clearChipSelectionEvent = MutableLiveData<Boolean>()
    // 칩 클릭 시 -> MainActivity의 SearchView(검색창)를 닫기 위한 이벤트
    val collapseSearchViewEvent = MutableLiveData<Boolean>() // 변수명 변경


    // --- SearchActivity 관련 코드 모두 제거 ---
    // private val _searchNewsList = ... (제거)
    // private val _searchLoadMoreEvent = ... (제거)
    // ... (관련 변수 및 함수 모두 제거)


    // --- 뉴스 로드 상태 관리 ---
    private var currentDefaultNewsPage = 1
    private val defaultNewsDisplayCount = 20
    private val trendingNewsDisplayCount = 10
    // currentQuery: 현재 로드된 뉴스의 쿼리 (검색어 또는 카테고리)
    private var currentQuery = "최신뉴스"
    private var isLoading = false // 중복 로드 방지 플래그


    init {
        // 앱 실행 시 "최신 뉴스" 로드
        searchNewsByCategory(currentQuery)
        // 트렌딩 뉴스 로드
        fetchTrendingNews(getApplication<Application>().getString(R.string.section_trending_news))
    }

    /**
     * [수정됨] 카테고리 칩 클릭 또는 SearchView 검색 시 호출되는 메인 함수
     * (기존 fetchDefaultNews(isLoadMore = false) 로직 통합)
     */
    fun searchNewsByCategory(query: String) {
        if (query.isBlank()) return
        if (isLoading) return
        isLoading = true
        _isLoadInProgress.value = true

        // 새 쿼리이므로 페이지 1부터 다시 시작
        currentQuery = query
        currentDefaultNewsPage = 1
        val startItemPosition = 1 // (currentDefaultNewsPage - 1) * defaultNewsDisplayCount + 1

        Log.d("NewsViewModel", "Starting new search for query: $query")

        viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(currentQuery, defaultNewsDisplayCount, startItemPosition)

                fetchedItems?.let { items ->
                    // 새 리스트이므로 'LoadMore'가 아닌 'defaultNewsList'에 값을 게시
                    _defaultNewsList.value = items

                    // 락스크린용 뉴스 저장
                    if (items.isNotEmpty()) {
                        LockScreenNewsManager.saveNews(getApplication(), items[0])
                    }
                } ?: run {
                    Log.e("NewsViewModel", "Error fetching news for query: $currentQuery (null response)")
                    _defaultNewsList.value = emptyList() // 오류 시 빈 리스트 처리
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error fetching news", e)
                _defaultNewsList.value = emptyList()
            } finally {
                isLoading = false
                _isLoadInProgress.value = false
            }
        }
    }

    /**
     * [수정됨] HomeFragment에서 스크롤이 끝에 닿았을 때 호출
     * (기존 fetchDefaultNews(isLoadMore = true) 로직 통합)
     */
    fun loadMoreDefaultNews() {
        if (isLoading) return // 이미 로드 중이면 중복 실행 방지

        isLoading = true
        _isLoadInProgress.value = true
        currentDefaultNewsPage++
        val startItemPosition = (currentDefaultNewsPage - 1) * defaultNewsDisplayCount + 1

        Log.d("NewsViewModel", "Loading more for query: $currentQuery, Page: $currentDefaultNewsPage")

        viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(currentQuery, defaultNewsDisplayCount, startItemPosition)

                fetchedItems?.let { items ->
                    // 추가 로드이므로 'loadMoreEvent'에 값을 게시
                    _loadMoreEvent.value = items
                } ?: run {
                    // 실패 시 페이지 번호 롤백
                    currentDefaultNewsPage--
                    Log.e("NewsViewModel", "Error loading more news (null response)")
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error loading more news", e)
                currentDefaultNewsPage-- // 실패 시 페이지 번호 롤백
            } finally {
                isLoading = false
                _isLoadInProgress.value = false
            }
        }
    }

    // --- fetchDefaultNews 함수 제거 ---
    // (위의 두 함수로 로직이 통합되었으므로 제거)
    // private fun fetchDefaultNews(isLoadMore: Boolean) { ... }


    /**
     * 트렌딩(인기) 뉴스 로드
     */
    fun fetchTrendingNews(query: String) {
        viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(query, display = trendingNewsDisplayCount, start = 1)
                _trendingNewsList.value = fetchedItems ?: emptyList()
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error fetching trending news", e)
            }
        }
    }

    // --- Search News Functions (모두 제거) ---
    // fun searchNews(query: String) { ... }
    // fun loadMoreSearchNews() { ... }
    // private fun fetchSearchNews(isLoadMore: Boolean) { ... }


    /**
     * 뉴스 스크랩 (DB에 저장/삭제)
     */
    fun toggleScrap(newsItem: NewsItem) {
        viewModelScope.launch {
            val normalizedLink = newsItem.originallink.ifEmpty { newsItem.link }
            val existingScrap = newsRepository.getScrappedNewsByLink(normalizedLink)
            if (existingScrap != null) {
                newsRepository.removeScrap(existingScrap)
                Log.d("ScrapToggle", "Scrap REMOVED (from toggle) for link: $normalizedLink")
            } else {
                newsRepository.addScrap(newsItem.toScrappedNewsItem())
                Log.d("ScrapToggle", "Scrap ADDED (from toggle) for link: $normalizedLink")
            }
        }
    }

    /**
     * 스크랩 탭에서 해제 전용 함수
     */
    fun removeScrap(scrappedItem: ScrappedNewsItem) {
        viewModelScope.launch {
            newsRepository.removeScrap(scrappedItem)
            Log.d("ScrapToggle", "Scrap REMOVED (from Scrapped Tab) for link: ${scrappedItem.link}")
        }
    }
}

