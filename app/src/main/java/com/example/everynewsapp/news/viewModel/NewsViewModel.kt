package com.example.everynewsapp.news.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.model.toScrappedNewsItem // Mapper import
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

    private var currentNewsPage = 1
    private val newsDisplayCount = 10
    private var currentQuery = "최신"
    private var isLoading = false

    init {
        searchNewsByCategory(currentQuery)
        fetchTrendingNews("인기 뉴스")
    }

    fun searchNewsByCategory(query: String) {
        currentQuery = query
        currentNewsPage = 1
        _defaultNewsList.value = emptyList()
        fetchDefaultNews(false)
    }

    fun loadMoreDefaultNews() {
        if (isLoading) return
        currentNewsPage++
        fetchDefaultNews(true)
    }

    private fun fetchDefaultNews(isLoadMore: Boolean) {
        if (isLoading) return
        isLoading = true
        val startItemPosition = (currentNewsPage - 1) * newsDisplayCount + 1

        viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(currentQuery, newsDisplayCount, startItemPosition)

                // fetchedItems가 null이 아닐 때만 처리하도록 수정
                fetchedItems?.let { items ->
                    if (isLoadMore) {
                        _defaultNewsList.value = _defaultNewsList.value.orEmpty() + items
                    } else {
                        _defaultNewsList.value = items
                    }

                    if (!isLoadMore && items.isNotEmpty()) {
                        LockScreenNewsManager.saveNews(getApplication(), items[0])
                    }
                } ?: run { // fetchedItems가 null일 경우
                    if (isLoadMore) currentNewsPage--
                    Log.e("NewsViewModel", "Error fetching default news, result is null")
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error fetching default news", e)
                if (isLoadMore) currentNewsPage--
            } finally {
                isLoading = false
            }
        }
    }

    private fun fetchTrendingNews(query: String) {
        viewModelScope.launch {
            try {
                val fetchedItems = NaverNewsApi.fetchNews(query, display = 15, start = 1)

                // --- 이 부분을 수정합니다 ---
                // API 결과가 null이면 빈 리스트로 대체하고, 아니면 그대로 사용
                _trendingNewsList.value = fetchedItems ?: emptyList()

            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error fetching trending news", e)
            }
        }
    }

    // --- 이 함수가 추가되었습니다 ---
    fun toggleScrap(newsItem: NewsItem) {
        viewModelScope.launch {
            val existingScrap = newsRepository.getNewsByLink(newsItem.originallink)
            if (existingScrap != null) {
                // 이미 스크랩된 경우: 삭제
                newsRepository.removeScrap(existingScrap)
            } else {
                // 스크랩되지 않은 경우: 추가
                newsRepository.addScrap(newsItem.toScrappedNewsItem())
            }
        }
    }
    // --- 여기까지 ---
}