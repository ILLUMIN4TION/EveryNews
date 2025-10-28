package com.example.everynewsapp.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.Html
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.everynewsapp.NotificationHelper
import com.example.everynewsapp.NotificationSettingsManager
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.network.NaverNewsApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Calendar // Calendar 임포트

class NewsNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "NewsNotificationWorker"
        // SharedPreferences 이름 및 키 (이전에 알림 보낸 뉴스 저장용)
        private const val PREFS_NAME = "NotificationWorkerPrefs"
        private const val KEY_LAST_NOTIFIED_LINK_PREFIX = "lastNotifiedLink_" // 키워드별 접두사
        private const val MAX_NOTIFICATIONS_PER_RUN = 3 // ★★★ 최대 알림 개수 상수 추가 ★★★
    }

    override suspend fun doWork(): Result = coroutineScope {
        Log.d(TAG, "백그라운드 키워드 뉴스 확인 작업 시작...")

        // --- 1. 알림 권한 확인 ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "알림 권한(POST_NOTIFICATIONS)이 없어 작업을 중단합니다.")
                // 권한이 없으면 더 이상 진행할 수 없으므로 실패 처리 또는 성공 처리 후 종료
                // 여기서는 성공으로 처리하여 WorkManager가 재시도하지 않도록 함
                return@coroutineScope Result.success()
            }
        }

        // --- 2. 알림 허용 시간 확인 ---
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) // 현재 시 (0-23)
        val startHour = NotificationSettingsManager.getNotificationStartHour(context)
        val endHour = NotificationSettingsManager.getNotificationEndHour(context)

        // 현재 시간이 설정된 범위 내에 있는지 확인
        val isTimeAllowed = if (startHour < endHour) {
            // 시작 시간이 종료 시간보다 작은 일반적인 경우 (예: 8시 ~ 22시)
            currentHour >= startHour && currentHour < endHour // 시작 시간은 포함, 종료 시간은 미포함
        } else if (startHour > endHour) {
            // 시간이 자정을 넘어가는 경우 (예: 22시 ~ 8시)
            currentHour >= startHour || currentHour < endHour
        } else {
            // startHour == endHour 인 경우는 없어야 하지만, 안전하게 false 처리
            Log.w(TAG, "알림 시작 시간과 종료 시간이 동일($startHour)합니다. 알림을 보내지 않습니다.")
            false
        }

        if (!isTimeAllowed) {
            Log.d(TAG, "현재 시간($currentHour)이 알림 허용 시간($startHour-$endHour)이 아니므로 알림을 보내지 않습니다.")
            return@coroutineScope Result.success() // 허용 시간이 아니면 성공 처리하고 종료
        }

        // --- 3. 등록된 키워드 확인 ---
        val keywords = NotificationSettingsManager.getKeywords(context)
        if (keywords.isEmpty()) {
            Log.d(TAG, "등록된 키워드가 없어 작업을 종료합니다.")
            // 키워드가 없으면 Worker를 계속 실행할 필요가 없으므로 Manager에서 취소하는 것이 더 좋음
            // NotificationSettingsManager.cancelNotificationWorker(context)
            return@coroutineScope Result.success()
        }

        // --- 4. 새 뉴스 검색 및 알림 발송 ---
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            // 각 키워드별로 뉴스 검색 (병렬 처리)
            val results = keywords.map { keyword ->
                async {
                    Log.d(TAG, "'$keyword' 키워드 뉴스 검색 중...")
                    // API 호출 시 적절한 개수(display) 요청 (예: 최신 5개)
                    NaverNewsApi.fetchNews(keyword, 5, 1)
                }
            }.awaitAll() // 모든 검색 결과 기다림

            // 키워드별 최신 '새' 뉴스를 저장할 맵
            val latestNewsMap = mutableMapOf<String, NewsItem>()

            // 검색 결과 처리
            results.filterNotNull().flatten().forEach { newsItem -> // null 제외, 리스트 평탄화
                keywords.forEach { keyword ->
                    // 뉴스가 키워드를 포함하는지 확인 (제목 또는 설명)
                    if (newsItem.title.contains(keyword, ignoreCase = true) ||
                        newsItem.description?.contains(keyword, ignoreCase = true) == true) {

                        // 이전에 알림 보낸 링크 가져오기
                        val lastNotifiedLinkKey = KEY_LAST_NOTIFIED_LINK_PREFIX + keyword
                        val lastNotifiedLink = prefs.getString(lastNotifiedLinkKey, null)
                        // 현재 뉴스 링크 (originallink 우선)
                        val currentNewsLink = newsItem.originallink.ifEmpty { newsItem.link }

                        // 해당 키워드로 찾은 첫 번째 '새' 뉴스만 저장 (이미 알림 보낸 링크 제외)
                        if (!latestNewsMap.containsKey(keyword) && currentNewsLink != lastNotifiedLink && currentNewsLink.isNotEmpty()) {
                            latestNewsMap[keyword] = newsItem
                            Log.d(TAG, "★ 새 뉴스 발견 ('$keyword'): ${newsItem.title.stripHtml()}")
                        }
                    }
                }
            }

            // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
            // ★★★ 수정: 알림 보낼 뉴스 선택 (최대 3개, 초과 시 무작위) ★★★
            val notificationsToSend: List<Pair<String, NewsItem>> = if (latestNewsMap.size > MAX_NOTIFICATIONS_PER_RUN) {
                // 3개를 초과하면, Map의 항목(Entry)을 리스트로 변환 후 섞어서 3개 선택
                latestNewsMap.entries.toList().shuffled().take(MAX_NOTIFICATIONS_PER_RUN).map { it.key to it.value }
            } else {
                // 3개 이하이면 모두 선택
                latestNewsMap.entries.toList().map { it.key to it.value }
            }
            // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

            // 선택된 뉴스에 대해서만 알림 보내고 SharedPreferences 업데이트
            if (notificationsToSend.isNotEmpty()) {
                val editor = prefs.edit()
                Log.d(TAG, "총 ${latestNewsMap.size}개의 새 뉴스 발견, ${notificationsToSend.size}개의 알림 전송 예정.")
                notificationsToSend.forEach { (keyword, newsItem) -> // Pair Destructuring
                    // NotificationHelper를 사용하여 알림 표시
                    NotificationHelper.showNotification(
                        context,
                        "'$keyword' 관련 새 뉴스!", // 알림 제목
                        newsItem.title.stripHtml() ?: "내용 없음", // 알림 내용 (HTML 제거)
                        newsItem // NewsItem 객체 전달 (PendingIntent용)
                    )
                    // 마지막으로 알림 보낸 링크 업데이트
                    val lastNotifiedLinkKey = KEY_LAST_NOTIFIED_LINK_PREFIX + keyword
                    val currentNewsLink = newsItem.originallink.ifEmpty { newsItem.link }
                    editor.putString(lastNotifiedLinkKey, currentNewsLink)
                    Log.d(TAG,"알림 전송 완료 및 마지막 링크 저장 ('$keyword'): $currentNewsLink")
                }
                editor.apply() // 변경사항 저장
            } else {
                Log.d(TAG, "확인된 키워드에 대한 새로운 뉴스가 없거나, 알림 보낼 뉴스가 선택되지 않았습니다.")
            }

            Log.d(TAG, "백그라운드 키워드 뉴스 확인 작업 완료.")
            Result.success() // 작업 성공

        } catch (e: Exception) {
            Log.e(TAG, "작업 중 오류 발생: ${e.message}", e)
            Result.retry() // 오류 발생 시 재시도 요청
        }
    }

    // HTML 태그 제거 유틸리티 함수
    private fun String?.stripHtml(): String {
        if (this == null) return ""
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION") // API 24 미만용
            Html.fromHtml(this).toString()
        }
    }
}

