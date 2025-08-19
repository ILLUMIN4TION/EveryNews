package com.example.everynewsapp.news.model

//뉴스 json 형식을 파싱할 데이터 클래스를 작성함
data class NewsResponse(
    val lastBuildDate: String,
    val total: Int,
    val start: Int,
    val display: Int,
    val items: List<NewsItem>
)
