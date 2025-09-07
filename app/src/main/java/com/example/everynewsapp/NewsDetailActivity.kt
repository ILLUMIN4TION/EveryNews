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

class NewsDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent로부터 NewsItem 객체 받아오기
        val newsItem = getNewsItemFromIntent()

        // 받아온 데이터가 있으면 UI에 내용 채우기
        if (newsItem != null) {
            populateUi(newsItem)
        } else {
            // 데이터가 없는 경우 에러 처리
            Log.e("NewsDetailActivity", "NewsItem 객체를 전달받지 못했습니다.")
            finish() // 액티비티 종료
        }
    }

    // Intent에서 데이터를 안전하게 추출하는 함수
    private fun getNewsItemFromIntent(): NewsItem? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("NEWS_ITEM", NewsItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<NewsItem>("NEWS_ITEM")
        }
    }

    // UI에 데이터를 채우는 함수
    private fun populateUi(item: NewsItem) {
        // 1. 제목 설정 (HTML 태그 제거)
        binding.tvDetailTitle.text = item.title.stripHtml()

        // 2. 내용 설정 (HTML 태그 제거)
        binding.tvDetailContent.text = item.description.stripHtml()

        // 3. 이미지 로딩 (Glide 사용)
        // NewsItem에 imageUrl이 없다면 작동하지 않으므로, 이 부분은 실제 데이터에 맞게 조정이 필요할 수 있습니다.
        Glide.with(this)
            .load(item.imageUrl) // 실제 이미지 URL 필드
            .placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지
            .error(R.drawable.ic_launcher_foreground) // 에러 시 이미지
            .into(binding.ivDetailImage)

        // 4. 원문 보기 버튼 클릭 이벤트
        binding.btnGoToOriginal.setOnClickListener {
            // originallink가 비어있지 않은 경우에만 브라우저 실행
            if (item.originallink.isNotEmpty()) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.originallink))
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
}