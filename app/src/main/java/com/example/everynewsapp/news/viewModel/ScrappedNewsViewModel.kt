package com.example.everynewsapp.news.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.model.ScrappedNewsItem
import com.example.everynewsapp.news.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ScrappedNewsViewModel(private val repository: NewsRepository) : ViewModel() {

    /**
     * Room 데이터베이스에서 스크랩된 모든 뉴스를 가져와 LiveData로 변환합니다.
     * UI는 이 LiveData를 관찰하여 데이터 변경 시 자동으로 화면을 업데이트합니다.
     */
    private val _isScrapped = MutableStateFlow(false)
    val isScrapped: StateFlow<Boolean> = _isScrapped.asStateFlow()
    val scrappedNews: LiveData<List<ScrappedNewsItem>> = repository.getAllScrappedNews().asLiveData()

    fun toggleScrap(newsItem: ScrappedNewsItem) {
        viewModelScope.launch {
            val currentLink = newsItem.originallink!!.ifEmpty { newsItem.link }
            if (repository.isScrapped(currentLink)) {
                // ScrappedNewsItem 객체 또는 고유 식별자(link)를 사용하여 삭제
                repository.removeScrap(currentLink)
                _isScrapped.value = false
            } else {
                // NewsItem을 ScrappedNewsItem으로 변환하여 추가
                repository.addScrap(newsItem) // Repository의 addScrap은 NewsItem을 받도록 구현되어 있어야 함
                _isScrapped.value = true
            }
        }

    }



}