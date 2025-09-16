//package com.example.everynewsapp.news.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.example.everynewsapp.news.repository.NewsRepository
//import com.example.everynewsapp.news.viewModel.ViewModels
//
//class ScrappedNewsViewModelFactory(private val repository: NewsRepository) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(ViewModels.ScrappedNewsViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return ViewModels().(repository) as T //
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}