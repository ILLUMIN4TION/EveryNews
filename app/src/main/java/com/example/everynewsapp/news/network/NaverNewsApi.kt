// NaverNewsApi.kt
package com.example.everynewsapp.news.network

import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.model.NewsResponse
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object NaverNewsApi {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val htmlFetcher = HtmlFetcher(client)
    private val imageCrawler = ImageCrawler()

    private const val CLIENT_ID = "sA74uxUzANP8rSi4SvSU"
    private const val CLIENT_SECRET = "cCy55viSiE"

    fun fetchNews(query: String, callback: (List<NewsItem>?) -> Unit) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://openapi.naver.com/v1/search/news.json?query=$encodedQuery&display=10&start=1&sort=date"

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Naver-Client-Id", CLIENT_ID)
            .addHeader("X-Naver-Client-Secret", CLIENT_SECRET)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                val newsResponse = gson.fromJson(body, NewsResponse::class.java)

                // 1. 뉴스 아이템 리스트 가져오기
                val initialNewsItems = newsResponse.items ?: emptyList()

                // 2. 제목에 한글이 포함된 뉴스만 필터링합니다.
                val koreanNewsItems = initialNewsItems.filter { newsItem ->
                    // HTML 태그를 제거하여 순수 텍스트만 필터링에 사용합니다.
                    val cleanTitle = newsItem.title.replace("<b>", "").replace("</b>", "")
                    // 정규식을 사용해 문자열에 한글이 하나라도 포함되어 있는지 확인합니다.
                    cleanTitle.matches(".*[가-힣].*".toRegex())
                }

                // 3. 필터링된 리스트를 사용해 이미지 크롤링을 진행합니다.
                val finalNewsItems = koreanNewsItems.map { newsItem ->
                    val newsLink = newsItem.originallink ?: newsItem.link
                    if (newsLink != null) {
                        val htmlContent = htmlFetcher.fetchHtml(newsLink)
                        if (htmlContent != null) {
                            val imageUrl = imageCrawler.extractImageUrl(htmlContent)
                            newsItem.imageUrl = imageUrl
                        }
                    }
                    newsItem
                }

                callback(finalNewsItems)

            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }
}