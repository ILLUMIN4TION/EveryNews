package com.example.everynewsapp

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "ThemePrefs"
    private const val KEY_THEME = "SelectedTheme"

    fun saveTheme(context: Context, themeMode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, themeMode).apply()
    }

    fun loadTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) // 기본값: 시스템 설정 따름
    }

    fun applyTheme(context: Context) {
        val themeMode = loadTheme(context)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
}