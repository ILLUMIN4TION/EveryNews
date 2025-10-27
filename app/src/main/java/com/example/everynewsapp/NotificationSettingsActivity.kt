package com.example.everynewsapp

// ★★★ START: import 블록이 정리되었습니다 ★★★
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.everynewsapp.databinding.ActivityNotificationSettingsBinding
import com.google.android.material.chip.Chip
import java.util.Calendar
import java.util.Locale
// ★★★ END: import 블록 ★★★

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding
    private val keywords = mutableSetOf<String>()
    private val MAX_KEYWORDS = 10

    // ★★★ START: REMOVED CODE ★★★
    // requestNotificationPermissionLauncher가 삭제되었습니다.
    // ★★★ END: REMOVED CODE ★★★

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this) // Apply theme first
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ★★★ START: ADDED CODE (유지) ★★★
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
        // ★★★ START: REMOVED CODE ★★★
        // 테스트 버튼 리스너가 삭제되었습니다.
        // ★★★ END: REMOVED CODE ★★★
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
    // ★★★ START: REMOVED FUNCTIONS ★★★
    // checkPermissionAndSendTest() 함수가 삭제되었습니다.
    // sendTestNotificationLogic() 함수가 삭제되었습니다.
    // isTimeInWindow() 함수가 삭제되었습니다.
    // ★★★ END: REMOVED FUNCTIONS ★★★
}