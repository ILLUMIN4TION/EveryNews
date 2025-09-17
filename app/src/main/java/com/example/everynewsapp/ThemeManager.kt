package com.example.everynewsapp

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "ThemePrefs"
    private const val KEY_THEME = "SelectedTheme"

    // 테마 모드를 저장하는 함수
    fun saveTheme(context: Context, themeMode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, themeMode).apply()
    }

    // 저장된 테마 모드를 불러오는 함수
    fun loadTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 기본값은 다크 모드로 설정
        return prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_YES)
    }

    // 앱 시작 시 테마를 적용하는 함수
    fun applyTheme(context: Context) {
        val themeMode = loadTheme(context)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
}