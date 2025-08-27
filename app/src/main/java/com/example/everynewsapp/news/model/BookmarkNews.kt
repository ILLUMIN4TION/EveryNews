package com.example.everynewsapp.news.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarked_news")
data class BookmarkedNews(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val link: String, //원문 주소
    val description: String, //내용
    val pubDate: String, //출간날짜
    val thumbnail: String? = null //썸네일 , 현재 미정
)
