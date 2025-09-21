package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.everynewsapp.databinding.ActivitySettingsBinding

// 클래스 바깥에 있던 불필요한 코드들을 모두 삭제했습니다.

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 테마 적용은 super.onCreate() 이전에 한 번만 호출합니다.
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // "앱 화면 설정" 레이아웃 클릭 이벤트
        binding.llThemeSetting.setOnClickListener {
            val intent = Intent(this, ThemeSettingsActivity::class.java)
            startActivity(intent)
        }

        // 현재 화면에 맞는 하단 바 아이콘 선택
        binding.bottomNavigationView.selectedItemId = R.id.navigation_settings

        // 하단 바 메뉴 클릭 이벤트
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_recommend -> {
                    startActivity(Intent(this, RecommendActivity::class.java))
                    true
                }
                R.id.navigation_scrap -> {
                    startActivity(Intent(this, ScrapActivity::class.java))
                    true
                }
                R.id.navigation_settings -> true // 현재 화면이므로 아무것도 하지 않음
                else -> false
            }
        }
    }
}
