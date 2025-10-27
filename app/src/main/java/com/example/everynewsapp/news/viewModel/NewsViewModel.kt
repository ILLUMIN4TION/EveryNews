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
import kotlinx.coroutines.Job // ★★★ 코루틴 Job 임포트 ★★★
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NewsViewModel(
    private val newsRepository: NewsRepository,
    application: Application
) : AndroidViewModel(application) {

    // --- HomeFragment가 관찰하는 LiveData ---
    // (기존 코드와 동일)
    private val _defaultNewsList = MutableLiveData<List<NewsItem>>()
    val defaultNewsList: LiveData<List<NewsItem>> get() = _defaultNewsList
    private val _trendingNewsList = MutableLiveData<List<NewsItem>>()
    val trendingNewsList: LiveData<List<NewsItem>> get() = _trendingNewsList
    private val _loadMoreEvent = MutableLiveData<List<NewsItem>>()
    val loadMoreEvent: LiveData<List<NewsItem>> get() = _loadMoreEvent
    private val _isLoadInProgress = MutableLiveData(false)
    val isLoadInProgress: LiveData<Boolean> get() = _isLoadInProgress
    private val _scrappedNewsList = newsRepository.getAllScrappedNews().asLiveData()
    val scrappedNewsList: LiveData<List<ScrappedNewsItem>> get() = _scrappedNewsList


    // ★★★ MainActivity <-> HomeFragment 통신용 LiveData ★★★
    // (기존 코드와 동일)
    val clearChipSelectionEvent = MutableLiveData<Boolean>()
    val collapseSearchViewEvent = MutableLiveData<Boolean>()


    // --- 뉴스 로드 상태 관리 ---
    private var currentDefaultNewsPage = 1
    private val defaultNewsDisplayCount = 20
    private val trendingNewsDisplayCount = 10
    private var currentQuery = getApplication<Application>().getString(R.string.section_latest_news)
    private var isLoading = false

    // ★★★ 현재 진행 중인 검색 작업을 추적하기 위한 Job 변수 추가 ★★★
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null // (더 보기도 Job으로 관리)


    init {
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        // ★★★ 수정: HomeFragment의 TabLayout이 로드를 트리거하므로 중복 호출 제거 ★★★
        // searchNewsByCategory(currentQuery)
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

        // 트렌딩 뉴스 로드는 유지
        fetchTrendingNews(getApplication<Application>().getString(R.string.section_trending_news))
    }

    /**
     * [수정됨] 카테고리 칩 클릭 또는 SearchView 검색 시 호출되는 메인 함수
     */
    fun searchNewsByCategory(query: String) {
        if (query.isBlank()) return

        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        // ★★★ 핵심 수정: isLoading 체크 대신, 기존 Job을 취소합니다. ★★★
        // if (isLoading) return // <-- 이 코드를 제거 (또는 주석 처리)

        // 1. 진행 중인 '새 검색' 또는 '더 보기' 작업을 모두 취소
        searchJob?.cancel()
        loadMoreJob?.cancel()
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

        // 2. 새 작업 시작을 위해 상태 설정
        isLoading = true
        _isLoadInProgress.value = true

        currentQuery = query
        currentDefaultNewsPage = 1
        val startItemPosition = 1

        Log.d("NewsViewModel", "Starting new search for query: $query")

        // 3. 새 작업을 searchJob 변수에 할당
        searchJob = viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(currentQuery, defaultNewsDisplayCount, startItemPosition)

                fetchedItems?.let { items ->
                    _defaultNewsList.value = items
                    if (items.isNotEmpty()) {
                        LockScreenNewsManager.saveNews(getApplication(), items[0])
                    }
                } ?: run {
                    Log.e("NewsViewModel", "Error fetching news for query: $currentQuery (null response)")
                    _defaultNewsList.value = emptyList()
                }
            } catch (e: Exception) {
                // (Job이 취소된 경우, CancellationException이 발생할 수 있으나 정상 동작임)
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("NewsViewModel", "Search for $query cancelled.")
                } else {
                    Log.e("NewsViewModel", "Error fetching news", e)
                    _defaultNewsList.value = emptyList()
                }
            } finally {
                // (Job이 정상적으로 완료되었을 때만 isLoading 해제)
                if(this.isActive) {
                    isLoading = false
                    _isLoadInProgress.value = false
                }
            }
        }
    }

    /**
     * [수정됨] HomeFragment에서 스크롤이 끝에 닿았을 때 호출
     */
    fun loadMoreDefaultNews() {
        // '새 검색'이 진행 중이거나 '더 보기'가 이미 진행 중이면 return
        if (isLoading) return

        isLoading = true
        _isLoadInProgress.value = true
        currentDefaultNewsPage++
        val startItemPosition = (currentDefaultNewsPage - 1) * defaultNewsDisplayCount + 1

        Log.d("NewsViewModel", "Loading more for query: $currentQuery, Page: $currentDefaultNewsPage")

        // '더 보기' 작업을 loadMoreJob 변수에 할당
        loadMoreJob = viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(currentQuery, defaultNewsDisplayCount, startItemPosition)

                fetchedItems?.let { items ->
                    _loadMoreEvent.value = items
                } ?: run {
                    currentDefaultNewsPage--
                    Log.e("NewsViewModel", "Error loading more news (null response)")
                }
            } catch (e: Exception) {
                // ★★★ 버그 수정: $query -> $currentQuery ★★★
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("NewsViewModel", "Load more for $currentQuery cancelled.")
                    currentDefaultNewsPage-- // 취소 시 페이지 롤백
                } else {
                    Log.e("NewsViewModel", "Error loading more news", e)
                    currentDefaultNewsPage--
                }
            } finally {
                if(this.isActive) {
                    isLoading = false
                    _isLoadInProgress.value = false
                }
            }
        }
    }

    /**
     * 트렌딩(인기) 뉴스 로드
     * (이 함수는 isLoading 플래그와 무관하게 독립적으로 실행되도록 둡니다)
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

    // (... toggleScrap, removeScrap 함수는 기존과 동일 ...)
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

