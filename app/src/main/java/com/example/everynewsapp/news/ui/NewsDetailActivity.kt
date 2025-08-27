package com.example.everynewsapp.news.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.everynewsapp.R

class NewsDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_detail)

        val thumbnailView: ImageView = findViewById(R.id.detailThumbnail)
        val titleView: TextView = findViewById(R.id.detailTitle)
        val descriptionView: TextView = findViewById(R.id.detailDescription)
        val dateView: TextView = findViewById(R.id.detailDate)
        val openButton: Button = findViewById(R.id.openWebButton)

        // Intent에서 데이터 받기
        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val pubDate = intent.getStringExtra("pubDate") ?: ""
        val link = intent.getStringExtra("link") ?: ""
        val thumbnailUrl = intent.getStringExtra("thumbnail") ?: ""

        // UI에 표시
        titleView.text = title
        descriptionView.text = description
        dateView.text = pubDate

        // 썸네일 이미지 로드 (없으면 기본 아이콘)
        if (thumbnailUrl.isNotEmpty()) {
            thumbnailView.load(thumbnailUrl)
        } else {
            thumbnailView.load("https://img.icons8.com/color/480/news.png")
        }

        // "원문 보기" 버튼 → WebViewActivity로 이동
        openButton.setOnClickListener {
            val intent = Intent(this, NewsWebViewActivity::class.java)
            intent.putExtra("link", link)
            startActivity(intent)
        }
    }
}
