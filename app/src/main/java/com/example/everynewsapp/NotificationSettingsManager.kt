package com.example.everynewsapp

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.everynewsapp.ui.NewsNotificationWorker
import java.util.concurrent.TimeUnit

object NotificationSettingsManager {
    private const val PREFS_NAME = "NotificationPrefs"
    private const val KEY_KEYWORDS = "keywords"
    private const val WORK_TAG = "newsNotificationWork"

    private const val KEY_START_HOUR = "notification_start_hour"
    private const val KEY_END_HOUR = "notification_end_hour"
    private const val DEFAULT_START_HOUR = 8
    private const val DEFAULT_END_HOUR = 22

    // ★★★ 추가: 스위치 상태 저장을 위한 키 ★★★
    private const val KEY_MASTER_SWITCH_ENABLED = "master_switch_enabled"
    private const val KEY_KEYWORD_SWITCH_ENABLED = "keyword_switch_enabled"

    // --- 키워드 관련 함수 ---
    // ★★★ 수정: 내부 저장 함수 (스케줄링 없음) ★★★
    private fun saveKeywordsInternal(context: Context, keywords: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_KEYWORDS, keywords).apply()
        Log.d("NotificationSettingsMgr", "Keywords saved internally.")
    }

    // 외부 호출용 키워드 저장 함수 (이제 스케줄링 안 함)
    fun saveKeywords(context: Context, keywords: Set<String>) {
        saveKeywordsInternal(context, keywords)
        // scheduleNotificationWorker(context) // <-- 자동 스케줄링 제거
    }

    fun getKeywords(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_KEYWORDS, emptySet()) ?: emptySet()
    }

    // ★★★ 수정: addKeyword에서 saveKeywordsInternal 사용 ★★★
    fun addKeyword(context: Context, keyword: String) {
        if (keyword.isBlank()) return
        val currentKeywords = getKeywords(context).toMutableSet()
        if (currentKeywords.add(keyword.trim())) {
            saveKeywordsInternal(context, currentKeywords) // 내부 저장 함수 호출
            Log.d("NotificationSettingsMgr", "Keyword '$keyword' added internally.")
        } else {
            Log.d("NotificationSettingsMgr", "Keyword '$keyword' already exists.")
        }
    }

    // ★★★ 수정: removeKeyword에서 saveKeywordsInternal 사용 및 자동 취소 제거 ★★★
    fun removeKeyword(context: Context, keyword: String) {
        val currentKeywords = getKeywords(context).toMutableSet()
        if (currentKeywords.remove(keyword)) {
            saveKeywordsInternal(context, currentKeywords) // 내부 저장 함수 호출
            Log.d("NotificationSettingsMgr", "Keyword '$keyword' removed internally.")
            // 키워드가 비어도 여기서 바로 취소하지 않음 (저장 버튼 누를 때 결정)
            // if (currentKeywords.isEmpty()) {
            //     cancelNotificationWorker(context)
            // }
        }
    }

    // --- 알림 시간 관련 함수 (기존과 동일) ---
    fun saveNotificationTime(context: Context, startHour: Int, endHour: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_START_HOUR, startHour)
            .putInt(KEY_END_HOUR, endHour)
            .apply()
        Log.d("NotificationSettingsMgr", "Notification time saved: $startHour - $endHour")
    }
    fun getNotificationStartHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_START_HOUR, DEFAULT_START_HOUR)
    }
    fun getNotificationEndHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_END_HOUR, DEFAULT_END_HOUR)
    }

    // ★★★ 추가: 스위치 상태 저장/로드 함수 ★★★
    fun saveSwitchStates(context: Context, isMasterEnabled: Boolean, isKeywordEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_MASTER_SWITCH_ENABLED, isMasterEnabled)
            .putBoolean(KEY_KEYWORD_SWITCH_ENABLED, isKeywordEnabled)
            .apply()
        Log.d("NotificationSettingsMgr", "Switch states saved: Master=$isMasterEnabled, Keyword=$isKeywordEnabled")
    }
    fun isMasterSwitchEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 기본값은 true로 설정 (앱 설치 후 처음엔 켜져 있도록)
        return prefs.getBoolean(KEY_MASTER_SWITCH_ENABLED, true)
    }
    fun isKeywordSwitchEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 기본값은 true로 설정
        return prefs.getBoolean(KEY_KEYWORD_SWITCH_ENABLED, true)
    }


    // --- Worker 스케줄링 및 취소 함수 ---
    // ★★★ 수정: 함수 이름 변경 및 조건부 실행 로직 추가 ★★★
    fun updateWorkerStateBasedOnSettings(context: Context) {
        val keywords = getKeywords(context)
        val isMasterEnabled = isMasterSwitchEnabled(context)
        val isKeywordEnabled = isKeywordSwitchEnabled(context)

        // Worker 실행 조건: 전체 알림 ON, 키워드 알림 ON, 키워드 1개 이상 등록
        if (isMasterEnabled && isKeywordEnabled && keywords.isNotEmpty()) {
            scheduleNotificationWorker(context) // 조건 만족 시 스케줄링
        } else {
            cancelNotificationWorker(context) // 조건 불만족 시 취소
            Log.d("NotificationSettingsMgr", "Worker condition not met (Master:$isMasterEnabled, Keyword:$isKeywordEnabled, Keywords:${keywords.size}). Worker cancelled or not scheduled.")
        }
    }

    // 실제 Worker 예약 로직 (private으로 변경 가능)
    private fun scheduleNotificationWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 주기는 30분 유지
        val periodicWorkRequest =
            PeriodicWorkRequestBuilder<NewsNotificationWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .build()

        // 정책은 REPLACE 유지
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
        Log.d("NotificationSettingsMgr", "Notification worker scheduled/replaced with 30 min interval.")
    }

    // 실제 Worker 취소 로직 (private으로 변경 가능)
    private fun cancelNotificationWorker(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
        Log.d("NotificationSettingsMgr", "Notification worker cancelled.")
    }
}

