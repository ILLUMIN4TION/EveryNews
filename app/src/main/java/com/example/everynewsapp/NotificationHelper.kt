package com.example.everynewsapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.everynewsapp.news.model.NewsItem // NewsItem 모델 임포트

object NotificationHelper {

    private const val CHANNEL_ID = "news_keyword_channel"
    private const val CHANNEL_NAME = "키워드 뉴스 알림"
    private const val CHANNEL_DESCRIPTION = "등록된 키워드 관련 새 뉴스 알림"
    private var channelCreated = false // 채널 생성 여부 플래그

    // 알림 채널 생성 (앱 시작 시 한 번만 호출 권장)
    fun createNotificationChannel(context: Context) {
        // Android O (API 26) 이상에서만 채널 생성 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !channelCreated) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT // 알림 중요도
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                // 추가 설정 가능: 진동, 소리 등
            }
            // 시스템에 채널 등록
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            channelCreated = true // 채널 생성됨 표시
            Log.d("NotificationHelper", "Notification channel created.")
        }
    }

    // 알림 표시
    fun showNotification(context: Context, title: String, content: String, newsItem: NewsItem? = null) {
        // 채널이 생성되었는지 확인 (안전 장치)
        createNotificationChannel(context)

        // 알림 클릭 시 실행될 Intent (NewsDetailActivity 실행)
        val intent = Intent(context, NewsDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // NewsItem 객체를 Intent에 추가 (Parcelable 또는 Serializable 필요)
            putExtra("NEWS_ITEM", newsItem)
        }
        // PendingIntent 생성 (알림 클릭 시 실행될 Intent 래핑)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            newsItem?.link?.hashCode() ?: 0, // 고유 요청 코드 (뉴스 링크 해시코드 사용)
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 빌더 생성
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_arrow_back) //TODO ★★★ 알림 아이콘 설정 필요 ★★★
            .setContentTitle(title) // 알림 제목
            .setContentText(content) // 알림 내용
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 알림 우선순위
            .setContentIntent(pendingIntent) // 알림 클릭 시 실행될 PendingIntent
            .setAutoCancel(true) // 클릭 시 알림 자동 제거

        // 알림 표시
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 알림 ID (각 알림을 구분하기 위한 고유 ID, 뉴스 링크 해시코드 사용)
        val notificationId = newsItem?.link?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())
        Log.d("NotificationHelper", "Notification shown for: $title")

        // --- 권장 사항: POST_NOTIFICATIONS 권한 확인 ---
        // Android 13 이상에서는 알림을 보내기 전에 이 권한이 있는지 확인해야 합니다.
        // Worker나 Activity에서 권한 확인 후 이 함수를 호출하는 것이 좋습니다.
        /*
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build())
            Log.d("NotificationHelper", "Notification shown for: $title")
        } else {
            Log.e("NotificationHelper", "POST_NOTIFICATIONS permission denied.")
        }
        */
    }
}
