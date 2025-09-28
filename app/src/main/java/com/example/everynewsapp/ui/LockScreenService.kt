package com.example.everynewsapp.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.everynewsapp.R

class LockScreenService : Service() {

    private val receiver = ScreenOnReceiver()

    // onStartConmand -> onStartCommand (m 하나 제거)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LockScreenCheck", ">>> LockScreenService가 시작되었습니다.")

        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(receiver, filter)

        // createNotifcationChannel -> createNotificationChannel (i 추가)
        createNotificationChannel()
        // NotifcationCompat -> NotificationCompat (i 추가)
        val notification = NotificationCompat.Builder(this, "lockscreen_service_channel")
            .setContentTitle("Every News")
            .setContentText("락 스크린 기능이 실행 중입니다.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    // createNotifcationChannel -> createNotificationChannel (i 추가)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "lockscreen_service_channel",
                "락 스크린 서비스 채널",
                NotificationManager.IMPORTANCE_MIN // 가장 낮은 등급으로 변경
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}