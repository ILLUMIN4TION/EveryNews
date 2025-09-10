package com.example.everynewsapp.news.viewModel

import androidx.activity.result.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.model.ScrappedNewsItem
import com.example.everynewsapp.news.model.ViewedNewsItem
import com.example.everynewsapp.news.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewModels {
    inner class ViewedNewsViewModel(private val repository: NewsRepository) : ViewModel() {
        val viewedNews: LiveData<List<ViewedNewsItem>> = repository.getAllViewedNews().asLiveData()

        fun clearHistory() {
            viewModelScope.launch {
                repository.clearAllViewedNews()
            }
        }
    }

    class ScrappedNewsViewModel(private val repository: NewsRepository) :
        ViewModel() {
        val scrappedNews: LiveData<List<ScrappedNewsItem>> = repository.getAllScrappedNews().asLiveData()

        // 스크랩 추가/삭제 로직은 뉴스 상세 화면이나 목록 아이템에서 처리할 수도 있음
    }

    // 뉴스 상세 화면 또는 메인 ViewModel에서 사용
    inner class NewsDetailViewModel(private val repository: NewsRepository) : ViewModel() {
        // ...

        fun onNewsViewed(newsItem: NewsItem) {
            viewModelScope.launch {
                repository.addViewedNews(newsItem)
            }
        }

        private val _isScrapped = MutableStateFlow(false)
        val isScrapped: StateFlow<Boolean> = _isScrapped.asStateFlow()

        fun checkScrapStatus(link: String) {
            viewModelScope.launch {
                _isScrapped.value = repository.isScrapped(link)
            }
        }

        fun toggleScrap(newsItem: NewsItem) {
            viewModelScope.launch {
                val currentLink = newsItem.originallink.ifEmpty { newsItem.link }
                if (repository.isScrapped(currentLink)) {
                    repository.removeScrap(currentLink)
                    _isScrapped.value = false
                } else {
                    repository.addScrap(newsItem)
                    _isScrapped.value = true
                }
            }
        }
    }

}