package com.example.everynewsapp.news.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.everynewsapp.news.model.BookmarkedNews

@Database(entities = [BookmarkedNews::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkedNewsDao(): BookmarkedNewsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "news_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
