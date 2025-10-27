package com.example.everynewsapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.everynewsapp.databinding.ActivityNewsDetailBinding
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.model.ScrappedNewsItem // ScrappedNewsItem import 추가
import java.text.SimpleDateFormat // ★★★ 추가 ★★★
import java.util.Date           // ★★★ 추가 ★★★
import java.util.Locale         // ★★★ 추가 ★★★

class NewsDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this) // 테마 적용
        super.onCreate(savedInstanceState)
        binding = ActivityNewsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val item = getFromIntent()

        if (item != null) {
            populateUi(item)
        } else {
            Log.e("NewsDetailActivity", "뉴스 객체를 전달받지 못했습니다.")
            finish()
        }
    }

    // Intent에서 두 가지 타입의 데이터를 모두 처리하는 함수 /일반, 스크랩뉴스
    private fun getFromIntent(): Any? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // "NEWS_ITEM" 키로 NewsItem 또는 ScrappedNewsItem을 시도
            intent.getParcelableExtra("NEWS_ITEM", NewsItem::class.java)
                ?: intent.getParcelableExtra("NEWS_ITEM", ScrappedNewsItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<NewsItem>("NEWS_ITEM")
                ?: intent.getParcelableExtra<ScrappedNewsItem>("NEWS_ITEM")
        }
    }

    //  UI에 데이터를 채우는 함수를 Any 타입으로 오버로드
    private fun populateUi(item: Any) {
        val title: String
        val description: String
        val imageUrl: String?
        val originalLink: String
        val pubDate: String // ★★★ 날짜 변수 추가 ★★★

        // item의 실제 타입에 따라 데이터를 추출
        when (item) {
            is NewsItem -> {
                title = item.title
                description = item.description
                imageUrl = item.imageUrl
                originalLink = item.originallink.ifEmpty { item.link }
                pubDate = item.pubDate // ★★★ 날짜 추출 ★★★
            }
            is ScrappedNewsItem -> {
                title = item.title
                description = item.description ?: ""
                imageUrl = item.imageUrl
                originalLink = item.originallink ?: item.link
                pubDate = item.pubDate // ★★★ 날짜 추출 ★★★
            }
            else -> return // 알 수 없는 타입이면 함수 종료
        }

        // 1. 제목 설정
        binding.tvDetailTitle.text = title.stripHtml()

        // ★★★ START: 날짜 및 출처 설정 코드 추가 ★★★
        binding.tvDetailDate.text = formatPubDate(pubDate)
        binding.tvDetailSource.text = getSourceFromLink(originalLink) // 링크에서 출처 추출
        // ★★★ END: 코드 추가 ★★★

        // 2. 내용 설정
        binding.tvDetailContent.text = description.stripHtml()

        // 3. 이미지 로딩
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지
            .error(R.drawable.ic_launcher_foreground)     // 에러 시 이미지
            .into(binding.ivDetailImage)

        // 4. 원문 보기 버튼
        binding.btnGoToOriginal.setOnClickListener {
            if (originalLink.isNotEmpty()) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(originalLink))
                startActivity(browserIntent)
            }
        }
    }

    // String 클래스에 HTML 태그를 제거하는 확장 함수 추가
    private fun String.stripHtml(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(this).toString()
        }
    }

    // ★★★ START: 날짜 형식 변환 함수 추가 ★★★
    private fun formatPubDate(pubDateString: String): String {
        // 네이버 API의 날짜 형식 예: "Mon, 27 May 2024 10:00:00 +0900"
        return try {
            val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val date = inputFormat.parse(pubDateString)
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            outputFormat.format(date ?: Date()) // 파싱 실패 시 현재 날짜
        } catch (e: Exception) {
            Log.e("NewsDetailActivity", "날짜 파싱 오류: $pubDateString", e)
            "날짜 정보 없음" // 오류 시 대체 텍스트
        }
    }
    // ★★★ END: 함수 추가 ★★★

    // ★★★ START: 링크에서 출처(도메인) 추출 함수 추가 ★★★
    private fun getSourceFromLink(link: String): String {
        return try {
            val uri = Uri.parse(link)
            // "www.example.com" -> "example.com"
            uri.host?.replaceFirst("www.", "") ?: "출처 정보 없음"
        } catch (e: Exception) {
            Log.e("NewsDetailActivity", "링크 파싱 오류: $link", e)
            "출처 정보 없음"
        }
    }
    // ★★★ END: 함수 추가 ★★★
}