package com.example.everynewsapp.news.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "viewed_news")
data class ViewedNewsItem(
    @PrimaryKey val link: String, // 뉴스의 고유 링크를 기본 키로 사용 (또는 별도 ID)
    val title: String,
    val originallink: String?,
    val description: String?,
    val pubDate: String?,
    val imageUrl: String?,
    val viewedAt: Long // 본 시간 (Timestamp)
)