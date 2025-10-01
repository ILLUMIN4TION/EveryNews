package com.example.everynewsapp.news.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.model.ScrappedNewsItem
import com.example.everynewsapp.news.network.NaverNewsApi
import com.example.everynewsapp.news.repository.NewsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.collections.flatten

class RecommendNewsViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _recommendedNewsList = MutableLiveData<List<NewsItem>>()
    val recommendedNewsList: LiveData<List<NewsItem>> get() = _recommendedNewsList

    init {
        fetchRecommendedNews()
    }

    private fun fetchRecommendedNews() {
        viewModelScope.launch {
            // 1. 스크랩된 뉴스 목록 가져오기
            val scrappedNews = repository.getAllScrappedNews().firstOrNull() ?: emptyList()

            // 2. 스크랩된 뉴스에서 상위 키워드 3개 추출
            val topKeywords = extractTopKeywords(scrappedNews)

            // 3. 각 키워드로 비동기 API 호출 및 결과 통합
            val fetchedResults: List<List<NewsItem>?> = topKeywords.map { keyword ->
                async {
                    // NaverNewsApi.fetchNews는 List<NewsItem>? (nullable List)를 반환합니다.
                    NaverNewsApi.fetchNews(query = keyword)
                }
            }.awaitAll()

            // 4. 결과를 합치고 중복 제거 (수정된 부분)
            val combinedList = fetchedResults
                .filterNotNull() // List<List<NewsItem>?>에서 null 값을 가진 리스트를 제거 -> List<List<NewsItem>>
                .flatten()       // 중첩된 리스트를 평탄화 -> List<NewsItem>
                .distinctBy { it.originallink } // 중복 제거

            // 5. 무작위로 섞어서 다양한 추천 효과를 냄
            val shuffledList = combinedList.shuffled()

            _recommendedNewsList.postValue(shuffledList)
        }
    }

    // 스크랩된 뉴스 목록에서 상위 키워드를 추출하는 함수
    private fun extractTopKeywords(scrappedNews: List<ScrappedNewsItem>): List<String> {
        val keywordScores = mutableMapOf<String, Int>()
        val keywords = listOf("IT", "테크", "AI", "반도체", "증시", "금융", "부동산", "스포츠", "야구", "축구", "정치", "사회", "문화")

        scrappedNews.forEach { news ->
            keywords.forEach { keyword ->
                if (news.title.contains(keyword, ignoreCase = true) || news.description?.contains(keyword, ignoreCase = true) == true) {
                    keywordScores[keyword] = keywordScores.getOrDefault(keyword, 0) + 1
                }
            }
        }
        return keywordScores.toList().sortedByDescending { (_, value) -> value }.toMap().keys.take(3).toList()
    }
}