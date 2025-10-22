package com.example.everynewsapp

// ★★★ START: 이 import 블록 전체를 복사하여 교체하세요 ★★★
import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.everynewsapp.databinding.ActivityNotificationSettingsBinding
import com.example.everynewsapp.news.model.NewsItem
import com.google.android.material.chip.Chip
import java.util.Calendar
import java.util.Locale
// ★★★ END: 여기까지 교체 ★★★

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding
    private val keywords = mutableSetOf<String>()
    private val MAX_KEYWORDS = 10

    // ★★★ START: ADDED CODE ★★★
    // 알림 권한 요청 런처
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 권한이 허용되면 테스트 로직 다시 실행
                sendTestNotificationLogic()
            } else {
                Toast.makeText(this, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    // ★★★ END: ADDED CODE ★★★

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this) // Apply theme first
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ★★★ START: ADDED CODE ★★★
        // (필수) 알림 채널 생성
        NotificationHelper.createNotificationChannel(this)
        // ★★★ END: ADDED CODE ★★★

        loadSettings()
        setupClickListeners()
    }

    private fun loadSettings() {
        val settings = NotificationSettingsManager.loadSettings(this)

        binding.switchMasterNotification.isChecked = settings.isMasterEnabled
        binding.tvStartTime.text = settings.startTime
        binding.tvEndTime.text = settings.endTime
        binding.switchKeywordNotification.isChecked = settings.isKeywordEnabled

        keywords.clear()
        keywords.addAll(settings.keywords)
        updateKeywordChips()
    }

    private fun setupClickListeners() {
        // Time Pickers
        binding.tvStartTime.setOnClickListener {
            showTimePicker(binding.tvStartTime, isStartTime = true)
        }
        binding.tvEndTime.setOnClickListener {
            showTimePicker(binding.tvEndTime, isStartTime = false)
        }

        // Add Keyword Button
        binding.btnAddKeyword.setOnClickListener {
            addKeywordFromInput()
        }

        // Add Keyword from Keyboard "Done"
        binding.etKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addKeywordFromInput()
                true
            } else {
                false
            }
        }

        // Save Button
        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }
        // ★★★ START: ADDED CODE ★★★
        // Test Button
        binding.btnTestNotification.setOnClickListener {
            checkPermissionAndSendTest()
        }
        // ★★★ END: ADDED CODE ★★★
    }

    private fun showTimePicker(textView: TextView, isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val currentText = textView.text.toString().split(":")
        val hour = currentText.getOrNull(0)?.toIntOrNull() ?: (if (isStartTime) 0 else 23)
        val minute = currentText.getOrNull(1)?.toIntOrNull() ?: (if (isStartTime) 0 else 59)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                textView.text = formattedTime
            },
            hour,
            minute,
            true // 24-hour format
        )
        timePickerDialog.show()
    }

    private fun addKeywordFromInput() {
        val keyword = binding.etKeyword.text.toString().trim()
        if (keyword.isEmpty()) {
            Toast.makeText(this, "키워드를 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (keywords.size >= MAX_KEYWORDS) {
            Toast.makeText(this, "키워드는 최대 $MAX_KEYWORDS 개까지 추가할 수 있습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (keywords.add(keyword)) {
            updateKeywordChips()
            binding.etKeyword.text.clear()
        } else {
            Toast.makeText(this, "이미 추가된 키워드입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateKeywordChips() {
        binding.chipGroupKeywords.removeAllViews()
        keywords.forEach { keyword ->
            val chip = Chip(this).apply {
                text = keyword
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    keywords.remove(keyword)
                    updateKeywordChips()
                }
            }
            binding.chipGroupKeywords.addView(chip)
        }
        updateKeywordCount()
    }

    private fun updateKeywordCount() {
        binding.tvKeywordCount.text = "${keywords.size}/$MAX_KEYWORDS 키워드"
    }

    private fun saveSettings() {
        val settings = NotificationSettingsManager.NotificationSettings(
            isMasterEnabled = binding.switchMasterNotification.isChecked,
            startTime = binding.tvStartTime.text.toString(),
            endTime = binding.tvEndTime.text.toString(),
            isKeywordEnabled = binding.switchKeywordNotification.isChecked,
            keywords = keywords
        )

        NotificationSettingsManager.saveSettings(this, settings)
        Toast.makeText(this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
        finish()
    }
    // ★★★ START: ADDED FUNCTIONS ★★★
    // 1. 권한 확인 및 테스트 시작
    private fun checkPermissionAndSendTest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 권한이 이미 있으면 알림 로직 실행
                    sendTestNotificationLogic()
                }
                else -> {
                    // 권한이 없으면 요청
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 이하는 권한 필요 없음
            sendTestNotificationLogic()
        }
    }

    // 2. 실제 테스트 알림 전송 로직
    private fun sendTestNotificationLogic() {
        // 현재 화면의 설정이 아닌, *저장된* 설정을 기준으로 테스트합니다.
        // (테스트 전 '설정 저장'을 눌러야 정확합니다)
        val settings = NotificationSettingsManager.loadSettings(this)

        if (!settings.isMasterEnabled) {
            Toast.makeText(this, "테스트 실패: '전체 알림'이 꺼져있습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isTimeInWindow(settings.startTime, settings.endTime)) {
            Toast.makeText(this, "테스트 실패: 현재 시간이 알림 시간대(${settings.startTime} ~ ${settings.endTime})가 아닙니다.", Toast.LENGTH_LONG).show()
            return
        }

        if (!settings.isKeywordEnabled || settings.keywords.isEmpty()) {
            Toast.makeText(this, "테스트 실패: '키워드 알림'이 꺼져있거나, 등록된 키워드가 없습니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 모든 조건 통과: 테스트 알림 생성
        val testKeyword = settings.keywords.first() // 첫 번째 키워드로 테스트
        val fakeNews = NewsItem(
            title = "[$testKeyword] 알림 테스트",
            originallink = "https://www.naver.com", // 테스트용 링크
            link = "https://www.naver.com",
            description = "이 알림은 '$testKeyword' 키워드 설정으로 인해 전송되었습니다.",
            pubDate = "",
            imageUrl = null // 테스트 알림에는 이미지 생략
        )

        NotificationHelper.sendKeywordNotification(this, fakeNews)
        Toast.makeText(this, "테스트 알림을 전송했습니다. (상단 바를 확인하세요)", Toast.LENGTH_SHORT).show()
    }

    // 3. 현재 시간이 설정된 시간 범위 내에 있는지 확인
    private fun isTimeInWindow(startTime: String, endTime: String): Boolean {
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
                // 일반적인 경우 (예: 08:00 ~ 22:00)
                nowInMinutes in startInMinutes..endInMinutes
            } else {
                // 자정을 걸친 경우 (예: 22:00 ~ 06:00)
                nowInMinutes >= startInMinutes || nowInMinutes <= endInMinutes
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false // 파싱 오류 시
        }
    }
    // ★★★ END: ADDED FUNCTIONS ★★★
}