package com.example.everynewsapp.news.network

import org.jsoup.Jsoup

class ImageCrawler {

    fun extractImageUrl(htmlContent: String): String? {
        val doc = Jsoup.parse(htmlContent)

        // 1. Open Graph 메타 태그에서 og:image 찾기
        val ogImageTag = doc.select("meta[property=og:image]").firstOrNull()
        if (ogImageTag != null) {
            return ogImageTag.attr("content")
        }

        // 2. og:image가 없으면 첫 번째 <img> 태그 찾기
        val firstImageTag = doc.select("img").firstOrNull()
        if (firstImageTag != null) {
            val src = firstImageTag.attr("src")
            return if (src.startsWith("http")) src else null
        }

        return null // 이미지를 찾지 못한 경우
    }
}