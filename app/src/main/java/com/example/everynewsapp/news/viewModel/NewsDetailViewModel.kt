//package com.example.everynewsapp.news.viewModel
//
//import androidx.activity.result.launch
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.everynewsapp.news.model.NewsItem
//import com.example.everynewsapp.news.model.toScrappedNewsItem
//import com.example.everynewsapp.news.repository.NewsRepository
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//
//
//class NewsDetailViewModel(private val repository: NewsRepository) : ViewModel() {
//
//
//
//    // 만약 뉴스 조회 시 기록하는 로직을 여기에 둔다면:
//    // fun onNewsViewed(newsItem: NewsItem) {
//    //     viewModelScope.launch {
//    //         // NewsItem을 ViewedNewsItem으로 변환하여 저장
//    //         // val viewedItem = ViewedNewsItem(...)
//    //         // repository.addViewedNews(viewedItem)
//    //     }
//    // }
//
//    private val _isScrapped = MutableStateFlow(false)
//    val isScrapped: StateFlow<Boolean> = _isScrapped.asStateFlow()
//
//    fun checkScrapStatus(link: String) {
//        viewModelScope.launch {
//            _isScrapped.value = repository.isScrapped(link)
//        }
//    }
//
//    fun toggleScrap(newsItem: NewsItem) {
//        viewModelScope.launch {
//            val link = newsItem.originallink.ifEmpty { newsItem.link }
//            val existingScrap = newsRepository.getScrappedNewsByLink(link)
//            if (existingScrap != null) {
//                // 이미 스크랩된 경우: 삭제
//                newsRepository.removeScrap(existingScrap)
//            } else {
//                // 스크랩되지 않은 경우: NewsItem을 ScrappedNewsItem으로 변환하여 Repository에 전달
//                val scrappedItem = newsItem.toScrappedNewsItem() // Mapper 함수 사용
//                newsRepository.addScrap(scrappedItem)
//            }
//        }
//    }
//}