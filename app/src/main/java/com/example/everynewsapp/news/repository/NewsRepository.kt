package com.example.everynewsapp.news.repository

import com.example.everynewsapp.news.dao.ScrappedNewsDao
//import com.example.everynewsapp.news.dao.ViewedNewsDao
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.model.ScrappedNewsItem
//import com.example.everynewsapp.news.model.ViewedNewsItem
import kotlinx.coroutines.flow.Flow

class NewsRepository(/*private val viewedNewsDao: ViewedNewsDao,*/ private val scrappedNewsDao: ScrappedNewsDao) {

    // 최근 본 뉴스 관련
//    fun getAllViewedNews(): Flow<List<ViewedNewsItem>> = viewedNewsDao.getAllViewedNews()
//
//    suspend fun addViewedNews(newsItem: NewsItem) { // NewsItem을 ViewedNewsItem으로 변환, 재활용
//        val viewedItem = ViewedNewsItem(
//            link = newsItem.originallink.ifEmpty { newsItem.link },
//            title = newsItem.title,
//            originallink = newsItem.originallink,
//            description = newsItem.description,
//            pubDate = newsItem.pubDate,
//            imageUrl = newsItem.imageUrl,
//            viewedAt = System.currentTimeMillis()
//        )
//        // 최대 개수 제한 로직 (예: 100개)
//        val currentCount = viewedNewsDao.getViewedNewsCount()
//        if (currentCount >= 100) {
//            viewedNewsDao.deleteOldestViewedNews(currentCount - 99) // 1개 초과분 삭제
//        }
//        viewedNewsDao.insertViewedNews(viewedItem)
//    }
//
//    suspend fun clearAllViewedNews() = viewedNewsDao.clearAllViewedNews() //추후 최근 본 뉴스를 없애고 싶을 때 사용합니다.





    // 스크랩 관련
    fun getAllScrappedNews(): Flow<List<ScrappedNewsItem>> = scrappedNewsDao.getAllScrappedNews()

    suspend fun addScrap(newsItem: NewsItem) {
        val scrappedItem = ScrappedNewsItem( /* ... ViewedNewsItem과 같이 NewsIem -> 스크랩 뉴스 아이템으로 변경합니다 ... */
            link = newsItem.originallink.ifEmpty { newsItem.link },
            title = newsItem.title,
            originallink = newsItem.originallink,
            description = newsItem.description,
            pubDate = newsItem.pubDate,
            imageUrl = newsItem.imageUrl,
            scrappedAt = System.currentTimeMillis()
        )
        scrappedNewsDao.insertScrappedNews(scrappedItem)
    }

    suspend fun addScrap(newsItem: ScrappedNewsItem) {
        val scrappedItem = ScrappedNewsItem( /* ... ViewedNewsItem과 같이 NewsIem -> 스크랩 뉴스 아이템으로 변경합니다 ... */
            link = newsItem.originallink!!.ifEmpty { newsItem.link },
            title = newsItem.title,
            originallink = newsItem.originallink,
            description = newsItem.description,
            pubDate = newsItem.pubDate,
            imageUrl = newsItem.imageUrl,
            scrappedAt = System.currentTimeMillis()
        )
        scrappedNewsDao.insertScrappedNews(scrappedItem)
    }

    suspend fun removeScrap(link: String) = scrappedNewsDao.deleteScrappedNews(link)

    suspend fun isScrapped(link: String): Boolean = scrappedNewsDao.getScrappedNewsByLink(link) != null //스크랩한 뉴스만 보여주기 위한 메서드입니다.

    // --- ViewModel이 정상 작동하기 위해 아래 함수들을 추가합니다 ---

    suspend fun getNewsByLink(link: String): ScrappedNewsItem? {
        return scrappedNewsDao.getScrappedNewsByLink(link)
    }

    suspend fun removeScrap(item: ScrappedNewsItem) {
        scrappedNewsDao.delete(item)
    }

}
