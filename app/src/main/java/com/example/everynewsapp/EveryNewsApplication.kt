package com.example.everynewsapp

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class EveryNewsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupPeriodicNewsCheck()
    }

    private fun setupPeriodicNewsCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val newsCheckWorkRequest =
            PeriodicWorkRequestBuilder<NewsNotificationWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NewsCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            newsCheckWorkRequest
        )
    }
}