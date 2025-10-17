package com.example.everynewsapp.news.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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

    private val _defaultNewsList = MutableLiveData<List<NewsItem>>()
    val defaultNewsList: LiveData<List<NewsItem>> get() = _defaultNewsList

    private val _trendingNewsList = MutableLiveData<List<NewsItem>>()
    val trendingNewsList: LiveData<List<NewsItem>> get() = _trendingNewsList

    private val _scrappedNewsList = newsRepository.getAllScrappedNews().asLiveData()
    val scrappedNewsList: LiveData<List<ScrappedNewsItem>> get() = _scrappedNewsList

    private val _loadMoreEvent = MutableLiveData<List<NewsItem>>()
    val loadMoreEvent: LiveData<List<NewsItem>> get() = _loadMoreEvent

    private val _isLoadInProgress = MutableLiveData(false)
    val isLoadInProgress: LiveData<Boolean> get() = _isLoadInProgress


    private var currentDefaultNewsPage = 1
    private val defaultNewsDisplayCount = 20
    private val trendingNewsDisplayCount = 10
    private var currentQuery = "최신"
    private var isLoading = false

    init {
        searchNewsByCategory(currentQuery)
        fetchTrendingNews("인기 뉴스")
    }

    fun searchNewsByCategory(query: String) {
        currentQuery = query
        currentDefaultNewsPage = 1
        _defaultNewsList.value = emptyList()
        fetchDefaultNews(false)
    }

    fun loadMoreDefaultNews() {
        if (isLoading) return
        currentDefaultNewsPage++
        fetchDefaultNews(true)
    }

    private fun fetchDefaultNews(isLoadMore: Boolean) {
        if (isLoading) return
        isLoading = true
        _isLoadInProgress.value = true
        val startItemPosition = (currentDefaultNewsPage - 1) * defaultNewsDisplayCount + 1

        viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(currentQuery, defaultNewsDisplayCount, startItemPosition)

                fetchedItems?.let { items ->
                    if (isLoadMore) {
                        _loadMoreEvent.value = items
                    } else {
                        _defaultNewsList.value = items
                    }

                    if (!isLoadMore && items.isNotEmpty()) {
                        LockScreenNewsManager.saveNews(getApplication(), items[0])
                    }
                } ?: run {
                    if (isLoadMore) currentDefaultNewsPage--
                    Log.e("NewsViewModel", "Error fetching default news, result is null")
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error fetching default news", e)
                if (isLoadMore) currentDefaultNewsPage--
            } finally {
                isLoading = false
                _isLoadInProgress.value = false
            }
        }
    }

    public fun fetchTrendingNews(query: String) {
        viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(query, display = trendingNewsDisplayCount, start = 1)
                _trendingNewsList.value = fetchedItems ?: emptyList()
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error fetching trending news", e)
            }
        }
    }

    // 메인 화면에서 스크랩 추가/삭제를 처리하는 기존 함수 (NewsItem 기반)
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

    // ★★★ 스크랩 탭에서 해제 전용 함수 추가 (ScrappedNewsItem 객체 기반) ★★★
    fun removeScrap(scrappedItem: ScrappedNewsItem) {
        viewModelScope.launch {
            newsRepository.removeScrap(scrappedItem)
            Log.d("ScrapToggle", "Scrap REMOVED (from Scrapped Tab) for link: ${scrappedItem.link}")
        }
    }
}
