package com.example.everynewsapp.ui

import android.content.Context
import com.example.everynewsapp.news.model.NewsItem

object LockScreenNewsManager {
    private const val PREFS_NAME = "LockScreenNewsPrefs"
    private const val KEY_TITLE = "news_title"
    private const val KEY_DESCRIPTION = "news_description"
    private const val KEY_IMAGE_URL = "news_image_url"
    private const val KEY_ORIGINAL_LINK = "news_original_link"

    fun saveNews(context: Context, newsItem: NewsItem) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_TITLE, newsItem.title)
            putString(KEY_DESCRIPTION, newsItem.description)
            putString(KEY_IMAGE_URL, newsItem.imageUrl)
            putString(KEY_ORIGINAL_LINK, newsItem.originallink)
            apply()
        }
    }

    fun loadNews(context: Context): Map<String, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return mapOf(
            "title" to prefs.getString(KEY_TITLE, "최신 뉴스를 불러오지 못했습니다."),
            "description" to prefs.getString(KEY_DESCRIPTION, ""),
            "imageUrl" to prefs.getString(KEY_IMAGE_URL, null),
            "originalLink" to prefs.getString(KEY_ORIGINAL_LINK, null)
        )
    }
}