package com.example.everynewsapp.news.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NewsItem(
    val title: String,
    val originallink: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val imageUrl: String? = null // imageUrl 필드가 없는 경우를 대비해 nullable로 유지
) : Parcelable