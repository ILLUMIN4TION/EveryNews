//package com.example.everynewsapp.news.viewModel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.example.everynewsapp.news.repository.NewsRepository
//import com.example.everynewsapp.news.viewModel.NewsDetailViewModel
//
//class NewsDetailViewModelFactory(private val repository: NewsRepository) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        // ViewModel 클래스가 NewsDetailViewModel과 동일한지 확인
//        if (modelClass.isAssignableFrom(NewsDetailViewModel::class.java)) {
//            // 동일하다면, repository를 생성자에 넣어 ViewModel 인스턴스 반환
//            @Suppress("UNCHECKED_CAST")
//            return NewsDetailViewModel(repository) as T
//        }
//        // 클래스가 다르면 예외 발생
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}