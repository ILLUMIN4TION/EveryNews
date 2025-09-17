package com.example.everynewsapp.news.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.model.ScrappedNewsItem
import com.example.everynewsapp.news.repository.NewsRepository


class ScrappedNewsViewModel(private val repository: NewsRepository) : ViewModel() {

    /**
     * Room 데이터베이스에서 스크랩된 모든 뉴스를 가져와 LiveData로 변환합니다.
     * UI는 이 LiveData를 관찰하여 데이터 변경 시 자동으로 화면을 업데이트합니다.
     */
    val scrappedNews: LiveData<List<ScrappedNewsItem>> = repository.getAllScrappedNews().asLiveData()
}