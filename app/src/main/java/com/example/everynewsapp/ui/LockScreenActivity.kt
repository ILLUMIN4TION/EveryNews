package com.example.everynewsapp.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.everynewsapp.NewsDetailActivity
import com.example.everynewsapp.R
import com.example.everynewsapp.ThemeManager
import com.example.everynewsapp.databinding.ActivityLockScreenBinding
import com.example.everynewsapp.news.model.NewsItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding
    private val timeHandler = Handler(Looper.getMainLooper())
    private var newsItemForDetail: NewsItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태바 영역까지 UI가 표시되도록 설정
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        updateTime()
        displayNews()
        setupSlideBar()
    }

    private fun displayNews() {
        val newsDataMap = LockScreenNewsManager.loadNews(this)
        newsItemForDetail = newsDataMap.toNewsItem() // 클릭 시 전달할 NewsItem 객체를 미리 생성

        val title = newsItemForDetail?.title
        binding.tvNewsTitle.text = title?.stripHtml() ?: "최신 뉴스가 없습니다."

        Glide.with(this)
            .load(newsItemForDetail?.imageUrl)
            .placeholder(R.drawable.ic_search) // 로딩 중 보일 기본 이미지
            .into(binding.ivNewsThumbnail)
    }

    private fun setupSlideBar() {
        binding.slideBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar == null) return

                when {
                    // 오른쪽으로 거의 끝까지 슬라이드 했을 때
                    seekBar.progress > 95 -> {
                        // 홈 화면으로 이동하는 Intent 생성
                        val intent = Intent(Intent.ACTION_MAIN)
                        intent.addCategory(Intent.CATEGORY_HOME)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish() // 락 스크린 액티비티 종료
                    }
                    // 왼쪽으로 거의 끝까지 슬라이드 했을 때
                    seekBar.progress < 5 -> {
                        // 뉴스 상세 페이지로 이동
                        if (newsItemForDetail != null) {
                            val intent = Intent(this@LockScreenActivity, NewsDetailActivity::class.java).apply {
                                putExtra("NEWS_ITEM", newsItemForDetail)
                            }
                            startActivity(intent)
                        }
                        finish() // 락 스크린 액티비티 종료
                    }
                    // 어중간하게 손을 뗐을 때
                    else -> {
                        // SeekBar를 다시 가운데(50)로 되돌리는 애니메이션
                        ObjectAnimator.ofInt(seekBar, "progress", seekBar.progress, 50).setDuration(300).start()
                    }
                }
            }
        })
    }

    // SharedPreferences에서 읽어온 Map을 NewsItem 객체로 변환하는 함수
    private fun Map<String, String?>.toNewsItem(): NewsItem? {
        val title = this["title"] ?: return null
        val originalLink = this["originalLink"] ?: return null
        val description = this["description"] ?: ""
        // NewsItem 객체 생성에 필요한 최소 정보로 구성
        return NewsItem(
            title = title,
            originallink = originalLink,
            link = originalLink,
            description = description,// 상세 정보는 NewsDetailActivity에서 필요 시 다시 로드
            pubDate = "",
            imageUrl = this["imageUrl"]
        )
    }

    private fun String.stripHtml(): String {
        return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
    }

    // 1분마다 시간 업데이트를 위한 코드
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            timeHandler.postDelayed(this, 60000)
        }
    }

    private fun updateTime() {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        val dateFormat = SimpleDateFormat("yyyy.MM.dd EEEE", Locale.getDefault())
        val now = Date()
        binding.tvTime.text = timeFormat.format(now)
        binding.tvDate.text = dateFormat.format(now).uppercase()
    }

    override fun onResume() {
        super.onResume()
        timeHandler.post(timeRunnable)
    }

    override fun onPause() {
        super.onPause()
        timeHandler.removeCallbacks(timeRunnable)
    }
}