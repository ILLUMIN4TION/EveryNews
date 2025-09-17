package com.example.everynewsapp.news.network

import android.util.Log
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.model.NewsResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object NaverNewsApi {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val htmlFetcher = HtmlFetcher(client)
    private val imageCrawler = ImageCrawler()

    // TODO: 보안을 위해 이 키들은 local.properties 파일로 옮기는 것을 강력히 추천합니다.
    private const val CLIENT_ID = "sA74uxUzANP8rSi4SvSU"
    private const val CLIENT_SECRET = "cCy55viSiE"

    // 콜백 방식 대신 코루틴을 위한 suspend 함수로 변경
    suspend fun fetchNews(query: String, display: Int = 10, start: Int = 1): List<NewsItem>? {
        // withContext를 사용해 백그라운드 스레드에서 네트워크 작업을 수행
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "https://openapi.naver.com/v1/search/news.json?query=$encodedQuery&display=10&start=1&sort=date"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("X-Naver-Client-Id", CLIENT_ID)
                    .addHeader("X-Naver-Client-Secret", CLIENT_SECRET)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                val newsResponse = gson.fromJson(body, NewsResponse::class.java)

                val initialNewsItems = newsResponse.items ?: emptyList()
                val koreanNewsItems = initialNewsItems.filter { newsItem ->
                    val cleanTitle = newsItem.title.replace("<b>", "").replace("</b>", "")
                    cleanTitle.matches(".*[가-힣].*".toRegex())
                }

                // coroutineScope를 사용해 모든 이미지 크롤링 작업을 동시에 병렬로 처리
                coroutineScope {
                    koreanNewsItems.map { newsItem ->
                        async {
                            val newsLink = newsItem.originallink.ifEmpty { newsItem.link }

                            if (newsLink.isEmpty() || !(newsLink.startsWith("http://") || newsLink.startsWith("https://"))) {
                                Log.w("NaverNewsApi", "Invalid or empty newsLink, skipping crawling: $newsLink")
                                return@async newsItem.copy(imageUrl = null)
                            }

                            val htmlContent = htmlFetcher.fetchHtml(newsLink)

                            val imageUrl = htmlContent?.let { contentOfHtml ->
                                imageCrawler.extractImageUrl(contentOfHtml, newsLink)
                            }

                            // 크롤링 실패 시 imageUrl은 null
                            newsItem.copy(imageUrl = imageUrl)
                        }
                    }.map { it.await() }
                        // ✅ 여기서 필터링: imageUrl이 null인 뉴스는 제외
                        .filter { it.imageUrl != null }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null // 오류 발생 시 null 반환
            }
        }
    }
}