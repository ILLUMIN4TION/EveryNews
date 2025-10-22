package com.example.everynewsapp

import android.content.Context
import android.content.SharedPreferences

object NotificationSettingsManager {

    private const val PREFS_NAME = "NotificationPrefs"
    private const val KEY_MASTER_ENABLED = "MasterEnabled"
    private const val KEY_START_TIME = "StartTime"
    private const val KEY_END_TIME = "EndTime"
    private const val KEY_KEYWORD_ENABLED = "KeywordEnabled"
    private const val KEY_KEYWORDS = "Keywords"

    // --- Data Class to hold all settings ---
    data class NotificationSettings(
        val isMasterEnabled: Boolean,
        val startTime: String,
        val endTime: String,
        val isKeywordEnabled: Boolean,
        val keywords: Set<String>
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Save all settings at once ---
    fun saveSettings(context: Context, settings: NotificationSettings) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_MASTER_ENABLED, settings.isMasterEnabled)
            putString(KEY_START_TIME, settings.startTime)
            putString(KEY_END_TIME, settings.endTime)
            putBoolean(KEY_KEYWORD_ENABLED, settings.isKeywordEnabled)
            putStringSet(KEY_KEYWORDS, settings.keywords)
            apply()
        }
    }

    // --- Load all settings ---
    fun loadSettings(context: Context): NotificationSettings {
        val prefs = getPrefs(context)
        return NotificationSettings(
            isMasterEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, false),
            startTime = prefs.getString(KEY_START_TIME, "00:00") ?: "00:00",
            endTime = prefs.getString(KEY_END_TIME, "23:59") ?: "23:59",
            isKeywordEnabled = prefs.getBoolean(KEY_KEYWORD_ENABLED, false),
            keywords = prefs.getStringSet(KEY_KEYWORDS, emptySet()) ?: emptySet()
        )
    }
}