package com.example.everynewsapp.news.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize


//스크랩한 뉴스를 담기 위한 데이터 클래스 입니다,
@Parcelize // ★ 추가 <- putExtra로 데이터 전달을 위해 넣었습니다.
@Entity(tableName = "scrapped_news")
data class ScrappedNewsItem(
    @PrimaryKey val link: String,
    val title: String,
    val originallink: String?,
    val description: String?,
    val pubDate: String?,
    val imageUrl: String?,
    val scrappedAt: Long // 스크랩한 시간 (Timestamp)
) : Parcelable