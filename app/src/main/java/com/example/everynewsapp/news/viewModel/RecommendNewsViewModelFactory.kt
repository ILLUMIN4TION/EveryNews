package com.example.everynewsapp.news.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.everynewsapp.news.repository.NewsRepository

class RecommendNewsViewModelFactory(private val repository: NewsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecommendNewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecommendNewsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}