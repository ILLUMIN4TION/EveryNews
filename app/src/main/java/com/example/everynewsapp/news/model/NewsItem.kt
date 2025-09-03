package com.example.everynewsapp.news.model


data class NewsItem(
    val title: String,
    val originallink: String,
    val link: String,
    val description: String,
    val pubDate: String,
    var imageUrl: String? //이미지가 들어올 수도 있고, 안들어 올수도 있으므로 널 허용합니다
)