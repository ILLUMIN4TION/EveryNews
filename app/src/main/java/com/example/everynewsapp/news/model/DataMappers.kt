package com.example.everynewsapp.news.model

// NewsItem(API용)을 ScrappedNewsItem(DB용)으로 변환
fun NewsItem.toScrappedNewsItem(): ScrappedNewsItem {
    return ScrappedNewsItem(
        uid = 0, // uid는 Room이 자동으로 생성
        title = this.title ?: "", // null일 경우를 대비
        originallink = this.originallink ?: "",
        link = this.link ?: "",
        description = this.description ?: "",
        pubDate = this.pubDate ?: "",
        imageUrl = this.imageUrl,
        scrappedAt = System.currentTimeMillis()
    )
}

// ScrappedNewsItem(DB용)을 NewsItem(API용)으로 변환
fun ScrappedNewsItem.toNewsItem(): NewsItem {
    return NewsItem(
        title = this.title,
        originallink = this.originallink,
        link = this.link,
        description = this.description,
        pubDate = this.pubDate,
        imageUrl = this.imageUrl
    )
}