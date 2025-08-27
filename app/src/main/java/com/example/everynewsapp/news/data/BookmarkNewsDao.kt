package com.example.everynewsapp.news.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import com.example.everynewsapp.news.model.BookmarkedNews


//db에 접근하기 위한 객체
@Dao
interface BookmarkedNewsDao {
    @Insert
    suspend fun insert(news: BookmarkedNews)

    @Delete
    suspend fun delete(news: BookmarkedNews)

    @Query("SELECT * FROM bookmarked_news")
    suspend fun getAll(): List<BookmarkedNews>
}
