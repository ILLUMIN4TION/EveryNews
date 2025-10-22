package com.example.everynewsapp

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

object NotificationSettingsManager {

    private const val PREFS_NAME = "NotificationPrefs"
    private const val KEY_MASTER_ENABLED = "MasterEnabled"
    private const val KEY_START_TIME = "StartTime"
    private const val KEY_END_TIME = "EndTime"
    private const val KEY_KEYWORD_ENABLED = "KeywordEnabled"
    private const val KEY_KEYWORDS = "Keywords"

    private const val PREFS_NOTIFIED_NEWS = "NotifiedNewsPrefs"
    private const val KEY_NOTIFIED_LINKS = "NotifiedLinks"
    private const val MAX_NOTIFIED_LINKS = 100

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

    // --- Background Worker Utility ---
    /**
     * Check if the current time is within the notification window.
     */
    fun isTimeInWindow(startTime: String, endTime: String): Boolean {
        try {
            val cal = Calendar.getInstance()
            val nowHour = cal.get(Calendar.HOUR_OF_DAY)
            val nowMinute = cal.get(Calendar.MINUTE)
            val nowInMinutes = nowHour * 60 + nowMinute

            val (startHour, startMin) = startTime.split(":").map { it.toInt() }
            val startInMinutes = startHour * 60 + startMin

            val (endHour, endMin) = endTime.split(":").map { it.toInt() }
            val endInMinutes = endHour * 60 + endMin

            return if (startInMinutes <= endInMinutes) {
                // Normal case (e.g., 08:00 ~ 22:00)
                nowInMinutes in startInMinutes..endInMinutes
            } else {
                // Spanning midnight (e.g., 22:00 ~ 06:00)
                nowInMinutes >= startInMinutes || nowInMinutes <= endInMinutes
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false // Parsing error
        }
    }

    // --- Notification Duplication Prevention ---
    private fun getNotifiedNewsPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NOTIFIED_NEWS, Context.MODE_PRIVATE)
    }

    /**
     * Get the set of previously notified news links.
     */
    fun getNotifiedLinks(context: Context): Set<String> {
        return getNotifiedNewsPrefs(context).getStringSet(KEY_NOTIFIED_LINKS, emptySet()) ?: emptySet()
    }

    /**
     * Save the set of notified news links (keeping only the latest 100).
     */
    fun saveNotifiedLinks(context: Context, newLinks: Set<String>) {
        // ★★★ MODIFIED: Convert Set to List before calling takeLast ★★★
        val updatedLinks = newLinks.toList().takeLast(MAX_NOTIFIED_LINKS).toSet()
        getNotifiedNewsPrefs(context).edit().apply {
            putStringSet(KEY_NOTIFIED_LINKS, updatedLinks)
            apply()
        }
    }
}