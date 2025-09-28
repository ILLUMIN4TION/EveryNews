package com.example.everynewsapp.news.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "scrapped_news")
data class ScrappedNewsItem(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
    val originallink: String,
    val title: String,
    val link: String,
    val pubDate: String,
    val description: String,
    val imageUrl: String?,
    val scrappedAt: Long
) : Parcelable