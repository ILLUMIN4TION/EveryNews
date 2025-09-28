package com.example.everynewsapp.news.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.everynewsapp.news.model.ScrappedNewsItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrappedNewsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScrappedNews(item: ScrappedNewsItem)

    // --- 이 함수를 추가합니다 ---
    @Delete
    suspend fun delete(item: ScrappedNewsItem)
    // --- 여기까지 ---

    @Query("SELECT * FROM scrapped_news ORDER BY scrappedAt DESC")
    fun getAllScrappedNews(): Flow<List<ScrappedNewsItem>>

    @Query("DELETE FROM scrapped_news WHERE link = :link")
    suspend fun deleteScrappedNews(link: String)

    @Query("SELECT * FROM scrapped_news WHERE link = :link") // 스크랩 여부 확인용
    suspend fun getScrappedNewsByLink(link: String): ScrappedNewsItem?
}