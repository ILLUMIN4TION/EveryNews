//package com.example.everynewsapp.news.dao
//
//import androidx.room.Dao
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import com.example.everynewsapp.news.model.ViewedNewsItem
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface ViewedNewsDao {
//    @Insert(onConflict = OnConflictStrategy.REPLACE) // 이미 본 뉴스를 다시 보면 viewedAt만 갱신
//    suspend fun insertViewedNews(item: ViewedNewsItem)
//
//    @Query("SELECT * FROM viewed_news ORDER BY viewedAt DESC") // 최근 본 순서대로 정렬하기
//    fun getAllViewedNews(): Flow<List<ViewedNewsItem>> // Flow로 비동기 데이터 스트림 <-
//
//    @Query("DELETE FROM viewed_news WHERE link = :link")
//    suspend fun deleteViewedNews(link: String)
//
//    @Query("DELETE FROM viewed_news")
//    suspend fun clearAllViewedNews()
//
//    @Query("SELECT COUNT(*) FROM viewed_news")
//    suspend fun getViewedNewsCount(): Int
//
//    @Query("DELETE FROM viewed_news WHERE viewedAt IN (SELECT viewedAt FROM viewed_news ORDER BY viewedAt ASC LIMIT :count)")
//    suspend fun deleteOldestViewedNews(count: Int) // 오래된 뉴스 삭제용
//}