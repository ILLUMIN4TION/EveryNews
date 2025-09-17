package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.everynewsapp.databinding.ActivityThemeSettingsBinding

class ThemeSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 현재 설정된 테마를 불러와 라디오 버튼에 표시
        when (ThemeManager.loadTheme(this)) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.rbLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.rbDark.isChecked = true
        }

        // 저장하기 버튼 클릭 리스너
        binding.btnSaveTheme.setOnClickListener {
            val selectedMode = when (binding.rgTheme.checkedRadioButtonId) {
                R.id.rb_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rb_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_YES // 기본값
            }

            // 선택된 테마를 저장하고 즉시 적용
            ThemeManager.saveTheme(this, selectedMode)
            AppCompatDelegate.setDefaultNightMode(selectedMode)

            // --- 아래 코드로 변경 ---
            // 앱을 재시작하여 테마를 완전히 적용합니다.
            val intent = Intent(this, MainActivity::class.java)
            // 기존에 쌓여있던 모든 액티비티를 종료하고, 새로운 시작점으로 설정
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish() // 설정 화면 닫기
        }
    }
}