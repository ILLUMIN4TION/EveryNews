package com.example.everynewsapp.news.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.everynewsapp.news.repository.NewsRepository

class NewsViewModelFactory(
    private val newsRepository: NewsRepository,
    private val application: Application // application 파라미터 추가
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // ViewModel 생성 시 application 전달
            return NewsViewModel(newsRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}