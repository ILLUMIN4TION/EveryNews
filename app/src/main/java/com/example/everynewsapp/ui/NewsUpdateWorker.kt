package com.example.everynewsapp.ui

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.network.NaverNewsApi

class NewsUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("NewsUpdateWorker", "백그라운드 뉴스 업데이트 작업 시작...")

        return try {
            // 1. API 호출 로직 통합
            val latestNewsItem = fetchLatestNewsFromApi()

            if (latestNewsItem != null) {
                // 2. LockScreenNewsManager를 사용하여 SharedPreferences에 저장
                LockScreenNewsManager.saveNews(applicationContext, latestNewsItem)
                Log.d("NewsUpdateWorker", "최신 뉴스 저장 성공: ${latestNewsItem.title}")
                Result.success()
            } else {
                Log.w("NewsUpdateWorker", "새로운 뉴스를 찾지 못함. 1시간 후 재시도.")
                Result.retry() // 재시도 설정
            }
        } catch (e: Exception) {
            Log.e("NewsUpdateWorker", "뉴스 업데이트 작업 실패", e)
            Result.failure()
        }
    }

    /**
     * NewsApi를 호출하여 가장 최신 뉴스 1개를 가져오는 함수.
     * (HomeFragment에서 사용하는 NewsViewModel 로직과 동일)
     */
    private suspend fun fetchLatestNewsFromApi(): NewsItem? {
        // "최신" 또는 "IT"와 같은 기본 쿼리를 사용합니다.
        val defaultQuery = "최신"
        val displayCount = 5 // LockScreen용으로 충분한 수량 (이미지 크롤링 후 1개라도 남을 확률 높임)

        return try {
            // NewsApi를 호출하여 크롤링과 필터링이 완료된 뉴스 목록을 받습니다.
            val fetchedItems = NaverNewsApi.fetchNews(defaultQuery, display = displayCount, start = 1)

            // 첫 번째 아이템을 반환합니다. (null이 아닌 경우)
            fetchedItems?.firstOrNull()

        } catch (e: Exception) {
            Log.e("NewsUpdateWorker", "API 호출 중 오류", e)
            null
        }
    }
}
