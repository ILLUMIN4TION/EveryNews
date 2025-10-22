package com.example.everynewsapp

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.network.NaverNewsApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class NewsNotificationWorker(
    val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "NewsNotificationWorker"
        // ★★★ 최대 알림 개수 상수 정의 (3개) ★★★
        private const val MAX_NOTIFICATIONS_PER_HOUR = 3
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "백그라운드 뉴스 확인 작업 시작...")
        try {
            // 1. 현재 알림 설정 불러오기
            val settings = NotificationSettingsManager.loadSettings(context)

            // 2. 전체 알림이 꺼져있으면 작업 종료
            if (!settings.isMasterEnabled) {
                Log.d(TAG, "전체 알림이 꺼져있어 작업을 중단합니다.")
                return Result.success()
            }

            // 3. 알림 허용 시간대가 아니면 작업 종료
            if (!NotificationSettingsManager.isTimeInWindow(settings.startTime, settings.endTime)) {
                Log.d(TAG, "알림 허용 시간대가 아니므로 작업을 중단합니다.")
                return Result.success()
            }

            // 4. 키워드 알림이 꺼져있거나 키워드가 없으면 작업 종료
            if (!settings.isKeywordEnabled || settings.keywords.isEmpty()) {
                Log.d(TAG, "키워드 알림이 꺼져있거나 키워드가 없어 작업을 중단합니다.")
                return Result.success()
            }

            // 5. 이전에 알림 보낸 링크 목록 불러오기 (중복 방지)
            val oldNotifiedLinks = NotificationSettingsManager.getNotifiedLinks(context)

            // 6. 모든 키워드에 대해 뉴스 API 병렬 호출 (키워드당 최신 5개만)
            val keywords = settings.keywords
            // ★★★ 키워드별 결과를 저장하도록 수정 ★★★
            val resultsPerKeyword = coroutineScope {
                keywords.map { keyword ->
                    async { keyword to NaverNewsApi.fetchNews(query = keyword, display = 5) }
                }.awaitAll() // [(keyword1, List<NewsItem>?), (keyword2, List<NewsItem>?), ...]
            }

            // ★★★ START: 7. 알림 보낼 뉴스 선택 로직 (랜덤 포함) ★★★
            val candidateNewsItems = mutableListOf<NewsItem>()

            resultsPerKeyword.forEach { (_, newsList) ->
                // 각 키워드별 결과에서 null이 아니고 비어있지 않은 리스트의 *첫 번째* (가장 최신) 뉴스만 후보에 추가
                newsList?.firstOrNull()?.let { latestNews ->
                    candidateNewsItems.add(latestNews)
                }
            }

            // 후보 뉴스 중에서 이전에 알림 보내지 않은 것만 필터링
            val newItemsToNotifyFiltered = candidateNewsItems
                .filter { it.originallink !in oldNotifiedLinks }
                .distinctBy { it.originallink } // 혹시 모를 중복 제거

            if (newItemsToNotifyFiltered.isEmpty()) {
                Log.d(TAG, "새로운 키워드 뉴스가 없습니다.")
                return Result.success()
            }

            // 키워드 개수에 따라 최종 알림 목록 선택
            val finalNewsItemsToSend = if (keywords.size > MAX_NOTIFICATIONS_PER_HOUR) {
                // 키워드가 3개 초과: 필터링된 새 뉴스 후보들을 무작위로 섞어서 최대 3개 선택
                newItemsToNotifyFiltered.shuffled().take(MAX_NOTIFICATIONS_PER_HOUR)
            } else {
                // 키워드가 3개 이하: 필터링된 새 뉴스 전부 (최대 3개)
                newItemsToNotifyFiltered
            }
            // ★★★ END: 7. 로직 수정 완료 ★★★


            // 8. 최종 선택된 뉴스에 대해 알림 전송
            Log.d(TAG, "${finalNewsItemsToSend.size}개의 새 뉴스를 발견하여 알림을 전송합니다.")
            val newLinks = mutableSetOf<String>()
            finalNewsItemsToSend.forEach { newsItem ->
                NotificationHelper.sendKeywordNotification(context, newsItem)
                newLinks.add(newsItem.originallink)
            }

            // 9. 알림 보낸 링크 목록을 SharedPreferences에 다시 저장
            val updatedLinks = oldNotifiedLinks + newLinks
            NotificationSettingsManager.saveNotifiedLinks(context, updatedLinks)

            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "백그라운드 작업 중 오류 발생", e)
            return Result.failure()
        }
    }
}