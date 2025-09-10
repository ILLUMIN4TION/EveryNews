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
    suspend fun fetchNews(query: String): List<NewsItem>? {
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
                    koreanNewsItems.map { newsItem -> // newsItem은 NewsItem 타입
                        async {
                            // newsLink는 크롤링할 원본 기사의 URL (http 또는 https로 시작해야 함)
                            val newsLink = newsItem.originallink.ifEmpty { newsItem.link }

                            // newsLink가 비어있거나 유효한 URL 형식이 아니면 크롤링을 건너뛸 수 있도록 방어 코드 추가
                            if (newsLink.isEmpty() || !(newsLink.startsWith("http://") || newsLink.startsWith("https://"))) {
                                Log.w("NaverNewsApi", "Invalid or empty newsLink, skipping crawling: $newsLink")
                                return@async newsItem // 이미지 없이 기존 newsItem 반환
                            }

                            val htmlContent = htmlFetcher.fetchHtml(newsLink)

                            // 여기가 54번째 줄 근처입니다.
                            val imageUrl = htmlContent?.let { contentOfHtml -> // it 대신 명시적인 이름 사용 (가독성 향상)
                                // 수정된 부분: 두 번째 인자로 newsLink (baseUrl)를 전달합니다.
                                imageCrawler.extractImageUrl(contentOfHtml, newsLink)
                            }

                            newsItem.copy(imageUrl = imageUrl)
                        }
                    }.map { it.await() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null // 오류 발생 시 null 반환
            }
        }
    }
}