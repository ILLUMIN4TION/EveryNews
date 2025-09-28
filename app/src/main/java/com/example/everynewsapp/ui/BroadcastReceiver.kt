package com.example.everynewsapp.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_ON) {
            val lockScreenIntent = Intent(context, LockScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(lockScreenIntent)
        }
    }
}