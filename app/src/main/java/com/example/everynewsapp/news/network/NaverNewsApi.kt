package com.example.everynewsapp.news.network

import com.example.everynewsapp.news.model.NewsResponse
import com.example.everynewsapp.news.model.NewsItem
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.net.URLEncoder

object NaverNewsApi {
    private val client = OkHttpClient()
    private val gson = Gson()

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

        //메인스레드에 네트워크 관련 작업 -> 예외발생 -> 별도의 스레드 사용
        Thread { // 람다, 매개변수 없고 본문만 있음
            try {
                val response = client.newCall(request).execute() // 받은 뉴스 응답을 저장
                val body = response.body?.string() // 응답에서 body를 빼냄
                val newsResponse = gson.fromJson(body, NewsResponse::class.java) // json으로 저장되어 있는 body를 클래스타입으로 바꾸고 새로 저장
                callback(newsResponse.items)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    } // fetchNewsEnd
}
