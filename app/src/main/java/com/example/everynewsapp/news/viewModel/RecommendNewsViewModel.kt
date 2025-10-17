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

class RecommendNewsViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _recommendedNewsList = MutableLiveData<List<NewsItem>>()
    val recommendedNewsList: LiveData<List<NewsItem>> get() = _recommendedNewsList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        fetchRecommendedNews()
    }

    // ★★★ 접근 지정자를 public으로 변경 (private 키워드 제거) ★★★
    fun fetchRecommendedNews() {
        if (_isLoading.value == true) return
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 1. 스크랩된 뉴스 목록 가져오기
                val scrappedNews = repository.getAllScrappedNews().firstOrNull() ?: emptyList()

                // 2. 스크랩된 뉴스에서 상위 키워드 3개 추출
                val topKeywords = extractTopKeywords(scrappedNews)

                // 3. 각 키워드로 비동기 API 호출 및 결과 통합
                val fetchedResults = topKeywords.map { keyword ->
                    async {
                        NaverNewsApi.fetchNews(query = keyword)
                    }
                }.awaitAll()

                // 4. 결과를 합치고 중복 제거
                val combinedList = fetchedResults.filterNotNull().flatten().distinctBy { it.originallink }

                // 5. 무작위로 섞어서 다양한 추천 효과를 냄
                val shuffledList = combinedList.shuffled()

                _recommendedNewsList.postValue(shuffledList)
            } catch (e: Exception) {
                // 오류 처리 로직 추가 가능
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 이 함수는 ViewModel 내부에서만 사용하므로 private으로 유지
    private fun extractTopKeywords(scrappedNews: List<ScrappedNewsItem>): List<String> {
        val keywordScores = mutableMapOf<String, Int>()
        val keywords = listOf("정치", "IT", "스포츠", "연예", "테크", "AI", "반도체", "증시", "금융", "부동산", "문화", "사회")

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
