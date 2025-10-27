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


    // ★★★ START: SearchActivity 전용 LiveData 추가 ★★★
    private val _searchNewsList = MutableLiveData<List<NewsItem>>()
    val searchNewsList: LiveData<List<NewsItem>> get() = _searchNewsList

    private val _searchLoadMoreEvent = MutableLiveData<List<NewsItem>>()
    val searchLoadMoreEvent: LiveData<List<NewsItem>> get() = _searchLoadMoreEvent
    // ★★★ END: SearchActivity 전용 LiveData 추가 ★★★


    // ★★★ MainActivity <-> HomeFragment 통신용 LiveData (제거됨) ★★★
    // clearChipSelectionEvent, collapseSearchViewEvent 제거


    // --- HomeFragment 뉴스 로드 상태 관리 ---
    private var currentDefaultNewsPage = 1
    private val defaultNewsDisplayCount = 20
    private val trendingNewsDisplayCount = 10
    private var currentQuery = getApplication<Application>().getString(R.string.section_latest_news)
    private var isLoading = false // 중복 로드 방지 플래그

    // ★★★ START: SearchActivity 전용 상태 변수 추가 ★★★
    private var currentSearchQuery = ""
    private var currentSearchNewsPage = 1
    private var isSearchLoading = false // 검색용 중복 로드 방지 플래그
    // ★★★ END: SearchActivity 전용 상태 변수 추가 ★★★


    init {
        // 앱 실행 시 "최신 뉴스" 로드
        searchNewsByCategory(currentQuery)
        // 트렌딩 뉴스 로드
        fetchTrendingNews(getApplication<Application>().getString(R.string.section_trending_news))
    }

    /**
     * [HomeFragment] 카테고리 칩 클릭 시 호출
     */
    fun searchNewsByCategory(query: String) {
        if (query.isBlank()) return
        if (isLoading) return
        isLoading = true
        _isLoadInProgress.value = true

        currentQuery = query
        currentDefaultNewsPage = 1
        val startItemPosition = 1

        Log.d("NewsViewModel", "Starting new category search: $query")

        viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(currentQuery, defaultNewsDisplayCount, startItemPosition)

                fetchedItems?.let { items ->
                    _defaultNewsList.value = items

                    if (items.isNotEmpty()) {
                        LockScreenNewsManager.saveNews(getApplication(), items[0])
                    }
                } ?: run {
                    Log.e("NewsViewModel", "Error fetching category news: $currentQuery (null response)")
                    _defaultNewsList.value = emptyList() // 오류 시 빈 리스트 처리
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error fetching category news", e)
                _defaultNewsList.value = emptyList()
            } finally {
                isLoading = false
                _isLoadInProgress.value = false
            }
        }
    }

    /**
     * [HomeFragment] 스크롤이 끝에 닿았을 때 호출
     */
    fun loadMoreDefaultNews() {
        if (isLoading) return // 이미 로드 중이면 중복 실행 방지

        isLoading = true
        _isLoadInProgress.value = true
        currentDefaultNewsPage++
        val startItemPosition = (currentDefaultNewsPage - 1) * defaultNewsDisplayCount + 1

        Log.d("NewsViewModel", "Loading more for category: $currentQuery, Page: $currentDefaultNewsPage")

        viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(currentQuery, defaultNewsDisplayCount, startItemPosition)

                fetchedItems?.let { items ->
                    _loadMoreEvent.value = items
                } ?: run {
                    currentDefaultNewsPage--
                    Log.e("NewsViewModel", "Error loading more category news (null response)")
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error loading more category news", e)
                currentDefaultNewsPage-- // 실패 시 페이지 번호 롤백
            } finally {
                isLoading = false
                _isLoadInProgress.value = false
            }
        }
    }

    /**
     * [HomeFragment] 트렌딩(인기) 뉴스 로드
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

    // ★★★ START: SearchActivity 전용 함수 추가 ★★★

    /**
     * [SearchActivity] 검색 버튼 클릭 시 호출
     */
    fun searchNews(query: String) {
        if (query.isBlank()) return
        if (isSearchLoading) return
        isSearchLoading = true
        _isLoadInProgress.value = true // ProgressBar 표시

        // 새 검색이므로 페이지 1부터 다시 시작
        currentSearchQuery = query
        currentSearchNewsPage = 1
        val startItemPosition = 1

        Log.d("NewsViewModel", "Starting new search for query: $query")

        viewModelScope.launch {
            try {
                // HomeFragment와 동일하게 20개씩 로드
                val fetchedItems = NaverNewsApi.fetchNews(currentSearchQuery, defaultNewsDisplayCount, startItemPosition)

                fetchedItems?.let { items ->
                    _searchNewsList.value = items
                } ?: run {
                    Log.e("NewsViewModel", "Error fetching search news (null response)")
                    _searchNewsList.value = emptyList() // 오류 시 빈 리스트
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error fetching search news", e)
                _searchNewsList.value = emptyList()
            } finally {
                isSearchLoading = false
                _isLoadInProgress.value = false
                _isLoadInProgress.value = false // ProgressBar 숨김
            }
        }
    }

    /**
     * [SearchActivity] 스크롤이 끝에 닿았을 때 호출
     */
    fun loadMoreSearchNews() {
        if (isSearchLoading) return // 이미 로드 중이면 중복 실행 방지

        isSearchLoading = true
        _isLoadInProgress.value = true // ProgressBar 표시
        currentSearchNewsPage++
        val startItemPosition = (currentSearchNewsPage - 1) * defaultNewsDisplayCount + 1

        Log.d("NewsViewModel", "Loading more search for query: $currentSearchQuery, Page: $currentSearchNewsPage")

        viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(currentSearchQuery, defaultNewsDisplayCount, startItemPosition)

                fetchedItems?.let { items ->
                    _searchLoadMoreEvent.value = items
                } ?: run {
                    currentSearchNewsPage-- // 실패 시 페이지 번호 롤백
                    Log.e("NewsViewModel", "Error loading more search news (null response)")
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error loading more search news", e)
                currentSearchNewsPage-- // 실패 시 페이지 번호 롤백
            } finally {
                isSearchLoading = false
                _isLoadInProgress.value = false // ProgressBar 숨김
            }
        }
    }
    // ★★★ END: SearchActivity 전용 함수 추가 ★★★


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