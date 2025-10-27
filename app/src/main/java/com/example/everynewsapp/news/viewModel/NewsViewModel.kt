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
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NewsViewModel(
    private val newsRepository: NewsRepository,
    application: Application
) : AndroidViewModel(application) {

    // --- LiveData 선언 ---
    private val _defaultNewsList = MutableLiveData<List<NewsItem>>()
    val defaultNewsList: LiveData<List<NewsItem>> get() = _defaultNewsList
    private val _trendingNewsList = MutableLiveData<List<NewsItem>>()
    val trendingNewsList: LiveData<List<NewsItem>> get() = _trendingNewsList
    private val _loadMoreEvent = MutableLiveData<List<NewsItem>>()
    val loadMoreEvent: LiveData<List<NewsItem>> get() = _loadMoreEvent
    private val _isLoadInProgress = MutableLiveData(false) // HomeFragment 로딩 상태
    val isLoadInProgress: LiveData<Boolean> get() = _isLoadInProgress
    private val _scrappedNewsList = newsRepository.getAllScrappedNews().asLiveData()
    val scrappedNewsList: LiveData<List<ScrappedNewsItem>> get() = _scrappedNewsList

    // ★★★ 추가: RecommendFragment 로딩 상태 LiveData ★★★
    private val _isRecommendLoading = MutableLiveData(false)
    val isRecommendLoading: LiveData<Boolean> get() = _isRecommendLoading

    // ★★★ 추가: RecommendFragment 뉴스 리스트 LiveData (예시) ★★★
    private val _recommendNewsList = MutableLiveData<List<NewsItem>>()
    val recommendNewsList: LiveData<List<NewsItem>> get() = _recommendNewsList


    // --- MainActivity <-> HomeFragment 통신용 LiveData ---
    val clearChipSelectionEvent = MutableLiveData<Boolean>()
    val collapseSearchViewEvent = MutableLiveData<Boolean>()

    // --- 뉴스 로드 상태 관리 ---
    private var currentDefaultNewsPage = 1
    private val defaultNewsDisplayCount = 20
    private val trendingNewsDisplayCount = 10
    private var currentQuery = getApplication<Application>().getString(R.string.section_latest_news)
    private var isLoading = false // HomeFragment 로딩 플래그
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null


    init {
        searchNewsByCategory(currentQuery)
        fetchTrendingNews(getApplication<Application>().getString(R.string.section_trending_news))
        // (선택) 앱 시작 시 추천 뉴스도 로드?
        // fetchRecommendNews()
    }

    // --- searchNewsByCategory, loadMoreDefaultNews (기존과 동일) ---
    fun searchNewsByCategory(query: String) {
        if (query.isBlank()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        _defaultNewsList.value = emptyList()
        isLoading = true
        _isLoadInProgress.value = true // Home 로딩 시작

        currentQuery = query
        currentDefaultNewsPage = 1
        val startItemPosition = 1
        Log.d("NewsViewModel", "Starting new search for query: $query")

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
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("NewsViewModel", "Search for $query cancelled.")
                } else {
                    Log.e("NewsViewModel", "Error fetching news", e)
                    isLoading = false
                    _isLoadInProgress.postValue(false) // Home 로딩 끝 (오류)
                }
            } finally {
                if(this.isActive) {
                    isLoading = false
                    _isLoadInProgress.value = false // Home 로딩 끝 (성공)
                } else {
                    Log.d("NewsViewModel", "Search job was cancelled, isLoading remains true for next job.")
                }
            }
        }
    }
    fun loadMoreDefaultNews() {
        if (isLoading) return
        isLoading = true
        _isLoadInProgress.value = true // Home 로딩 시작
        currentDefaultNewsPage++
        val startItemPosition = (currentDefaultNewsPage - 1) * defaultNewsDisplayCount + 1
        Log.d("NewsViewModel", "Loading more for query: $currentQuery, Page: $currentDefaultNewsPage")

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
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("NewsViewModel", "Load more for $currentQuery cancelled.")
                    currentDefaultNewsPage--
                } else {
                    Log.e("NewsViewModel", "Error loading more news", e)
                    currentDefaultNewsPage--
                    isLoading = false
                    _isLoadInProgress.postValue(false) // Home 로딩 끝 (오류)
                }
            } finally {
                if(this.isActive) {
                    isLoading = false
                    _isLoadInProgress.value = false // Home 로딩 끝 (성공)
                } else {
                    Log.d("NewsViewModel", "Load more job was cancelled, isLoading remains true for next job.")
                }
            }
        }
    }


    // --- fetchTrendingNews (기존과 동일) ---
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

    // ★★★ 추가: 추천 뉴스 로드 함수 (예시) ★★★
    fun fetchRecommendNews() {
        _isRecommendLoading.value = true // 추천 로딩 시작
        viewModelScope.launch {
            try {
                // TODO: 실제 추천 뉴스 API 호출 로직 구현
                // 예시: val recommendItems = newsRepository.getRecommendedNews(...)
                // 임시로 트렌딩 뉴스를 재사용
                val recommendItems = NaverNewsApi.fetchNews(getApplication<Application>().getString(R.string.section_trending_news), 15, 1)
                _recommendNewsList.value = recommendItems ?: emptyList()
                Log.d("NewsViewModel", "Fetched recommend news")
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error fetching recommend news", e)
                _recommendNewsList.value = emptyList() // 오류 시 빈 리스트
            } finally {
                _isRecommendLoading.value = false // 추천 로딩 끝
            }
        }
    }


    // --- toggleScrap, removeScrap (기존과 동일) ---
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
    fun removeScrap(scrappedItem: ScrappedNewsItem) {
        viewModelScope.launch {
            newsRepository.removeScrap(scrappedItem)
            Log.d("ScrapToggle", "Scrap REMOVED (from Scrapped Tab) for link: ${scrappedItem.link}")
        }
    }
}

