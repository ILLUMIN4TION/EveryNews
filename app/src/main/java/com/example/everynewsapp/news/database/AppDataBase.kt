package com.example.everynewsapp.news.database
import android.content.Context
import androidx.room.Database
import com.example.everynewsapp.news.dao.ScrappedNewsDao
//import com.example.everynewsapp.news.dao.ViewedNewsDao
import com.example.everynewsapp.news.model.ScrappedNewsItem
//import com.example.everynewsapp.news.model.ViewedNewsItem

@Database(entities = [ScrappedNewsItem::class], version = 2, exportSchema = false)
abstract class AppDatabase : androidx.room.RoomDatabase() {
//    abstract fun viewedNewsDao(): ViewedNewsDao
    abstract fun scrappedNewsDao(): ScrappedNewsDao

    companion object { //싱글톤 객체를 구성하기 위한 companion object입니다, 앱 내에서 하나의 인스턴스만 생성하여 관리합니다.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "news_app_database"  //데이터베이스의 실제 이름입니다.
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
