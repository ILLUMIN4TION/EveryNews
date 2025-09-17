package com.example.everynewsapp.news.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.everynewsapp.news.repository.NewsRepository

class ScrappedNewsViewModelFactory(private val repository: NewsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // 요청된 클래스가 ScrappedNewsViewModel인지 확인
        if (modelClass.isAssignableFrom(ScrappedNewsViewModel::class.java)) {
            // 맞다면 ScrappedNewsViewModel 인스턴스를 생성하여 반환
            @Suppress("UNCHECKED_CAST")
            return ScrappedNewsViewModel(repository) as T
        }
        // 요청된 클래스가 다르면 예외를 발생
        throw IllegalArgumentException("Unknown ViewModel class")
    }


}