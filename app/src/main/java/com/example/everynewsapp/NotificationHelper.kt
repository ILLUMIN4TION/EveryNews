package com.example.everynewsapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.everynewsapp.news.model.NewsItem

object NotificationHelper {

    private const val CHANNEL_ID = "keyword_news_channel"
    private const val CHANNEL_NAME = "키워드 알림"
    private const val NOTIFICATION_ID_PREFIX = 12345 // 알림 ID 접두사

    // 1. (앱 실행 시) 알림 채널 생성
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "설정한 키워드에 대한 새 소식 알림"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 2. 키워드 알림 전송
    fun sendKeywordNotification(context: Context, newsItem: NewsItem) {
        // 알림 클릭 시 NewsDetailActivity로 이동하는 Intent 생성
        val intent = Intent(context, NewsDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NEWS_ITEM", newsItem) // NewsItem 객체 전달
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(), // 고유한 requestCode
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 알림 아이콘
            .setContentTitle(newsItem.title) // 알림 제목
            .setContentText(newsItem.description) // 알림 내용
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // 클릭 시 실행할 Intent
            .setAutoCancel(true) // 클릭하면 알림 자동 삭제

        // 3. 알림 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // 권한이 없으면 전송하지 않음 (테스트 버튼 로직에서 이미 권한을 요청하므로 여기서는 생략)
                return
            }
        }

        // 4. 알림 표시
        with(NotificationManagerCompat.from(context)) {
            // 각 뉴스마다 고유한 ID를 갖도록 hashCode 사용
            val notificationId = NOTIFICATION_ID_PREFIX + (newsItem.originallink.hashCodeOr(System.currentTimeMillis().toInt()))
            notify(notificationId, builder.build())
        }
    }
}

// String.hashCode()가 음수일 경우를 대비한 헬퍼
private fun String?.hashCodeOr(default: Int): Int {
    return this?.hashCode() ?: default
}