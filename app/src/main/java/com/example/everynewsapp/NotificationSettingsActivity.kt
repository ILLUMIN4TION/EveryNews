package com.example.everynewsapp

import android.Manifest
import android.app.TimePickerDialog // TimePickerDialog 임포트
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log // Log 임포트
import android.view.LayoutInflater // LayoutInflater 임포트
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.everynewsapp.databinding.ActivityNotificationSettingsBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial // SwitchMaterial 임포트
import java.util.Calendar // Calendar 임포트
import java.util.Locale // Locale 임포트

class NotificationSettingsActivity : AppCompatActivity() {

    val binding by lazy { ActivityNotificationSettingsBinding.inflate(layoutInflater) }

    // UI 요소 변수 선언
    private lateinit var switchMasterNotification: SwitchMaterial
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var switchKeywordNotification: SwitchMaterial
    private lateinit var etKeyword: EditText
    private lateinit var btnAddKeyword: Button
    private lateinit var chipGroupKeywords: ChipGroup
    private lateinit var tvKeywordCount: TextView
    private lateinit var btnSaveSettings: Button // 저장 버튼

    // 현재 설정된 시간 저장 변수
    private var currentStartHour: Int = 0
    private var currentEndHour: Int = 0

    // ★★★ 임시 저장 변수 제거 ★★★
    // private var isMasterEnabled = true
    // private var isKeywordEnabled = true

    // 권한 요청 런처
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
                // 권한 허용 시 스위치 상태 반영 (UI만 업데이트, 저장은 버튼 클릭 시)
                switchMasterNotification.isChecked = true
                updateKeywordSectionEnabledState() // 키워드 섹션 상태 업데이트
            } else {
                Toast.makeText(this, "알림 권한이 거부되었습니다. 알림을 받을 수 없습니다.", Toast.LENGTH_LONG).show()
                // 권한 거부 시 스위치 강제 끄기 (UI만 업데이트)
                switchMasterNotification.isChecked = false
                switchKeywordNotification.isChecked = false
                updateKeywordSectionEnabledState() // 키워드 섹션 비활성화
                // NotificationSettingsManager.cancelNotificationWorker(this) // <-- 저장 버튼 누를 때 처리
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this) // 테마 적용
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // UI 요소 초기화 (findViewById 사용)
        switchMasterNotification = findViewById(R.id.switch_master_notification)
        tvStartTime = findViewById(R.id.tv_start_time)
        tvEndTime = findViewById(R.id.tv_end_time)
        switchKeywordNotification = findViewById(R.id.switch_keyword_notification)
        etKeyword = findViewById(R.id.et_keyword)
        btnAddKeyword = findViewById(R.id.btn_add_keyword)
        chipGroupKeywords = findViewById(R.id.chip_group_keywords)
        tvKeywordCount = findViewById(R.id.tv_keyword_count)
        btnSaveSettings = findViewById(R.id.btn_save_settings)

        loadSettings() // 저장된 설정 로드
        setupListeners() // 이벤트 리스너 설정
        checkAndRequestNotificationPermission() // 알림 권한 확인 및 요청 (필요시)
    }

    // 저장된 설정 로드 및 UI 반영
    private fun loadSettings() {
        // 시간 로드 및 표시
        currentStartHour = NotificationSettingsManager.getNotificationStartHour(this)
        currentEndHour = NotificationSettingsManager.getNotificationEndHour(this)
        updateTimeTextViews()

        // 키워드 로드 및 표시
        chipGroupKeywords.removeAllViews()
        val keywords = NotificationSettingsManager.getKeywords(this)
        keywords.forEach { addChipToGroup(it) }
        updateKeywordCount()

        // 스위치 상태 로드 및 반영
        switchMasterNotification.isChecked = NotificationSettingsManager.isMasterSwitchEnabled(this)
        switchKeywordNotification.isChecked = NotificationSettingsManager.isKeywordSwitchEnabled(this)
        // 스위치 상태에 따라 UI 활성화/비활성화
        updateKeywordSectionEnabledState()
    }

    // 시간 TextView 업데이트
    private fun updateTimeTextViews() {
        // 시간 포맷 (예: 08:00)
        tvStartTime.text = String.format(Locale.getDefault(), "%02d:00", currentStartHour)
        tvEndTime.text = String.format(Locale.getDefault(), "%02d:00", currentEndHour)
    }

    // 이벤트 리스너 설정
    private fun setupListeners() {
        // 전체 알림 스위치
        switchMasterNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 켤 때: 권한 확인
                if (!checkNotificationPermission()) {
                    requestNotificationPermission()
                    // 권한 없으면 즉시 다시 끔 (런처 콜백에서 UI 처리)
                    switchMasterNotification.isChecked = false
                    return@setOnCheckedChangeListener // 더 이상 진행 안 함
                }
            }
            // 스위치 상태 변경 시 키워드 섹션 UI 업데이트
            updateKeywordSectionEnabledState()
            Log.d("NotiSettingsActivity", "Master switch UI changed: $isChecked")
            // ★★★ 여기서 Worker 상태 바로 변경 안 함 ★★★
        }

        // 키워드 알림 스위치
        switchKeywordNotification.setOnCheckedChangeListener { _, isChecked ->
            // 스위치 상태 변경 시 키워드 섹션 UI 업데이트
            updateKeywordSectionEnabledState()
            Log.d("NotiSettingsActivity", "Keyword switch UI changed: $isChecked")
            // ★★★ 여기서 Worker 상태 바로 변경 안 함 ★★★
        }

        // 시작 시간 TextView 클릭 리스너
        tvStartTime.setOnClickListener { showTimePickerDialog(true) }

        // 종료 시간 TextView 클릭 리스너
        tvEndTime.setOnClickListener { showTimePickerDialog(false) }

        // 키워드 추가 버튼 리스너
        btnAddKeyword.setOnClickListener { addKeywordFromInput() }
        etKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addKeywordFromInput()
                true
            } else { false }
        }

        // 설정 저장 버튼 리스너
        btnSaveSettings.setOnClickListener {
            // 1. 현재 UI 상태(시간, 스위치)를 저장
            NotificationSettingsManager.saveNotificationTime(this, currentStartHour, currentEndHour)
            NotificationSettingsManager.saveSwitchStates(this, switchMasterNotification.isChecked, switchKeywordNotification.isChecked)
            // 키워드는 추가/삭제 시 NotificationSettingsManager에 이미 반영됨 (saveKeywordsInternal 사용)

            // 2. 저장된 최종 설정을 바탕으로 Worker 상태 업데이트 (예약 또는 취소)
            NotificationSettingsManager.updateWorkerStateBasedOnSettings(this)

            Toast.makeText(this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            finish() // 액티비티 종료
        }
    }

    // TimePickerDialog 표시 함수 (기존과 동일)
    private fun showTimePickerDialog(isStartTime: Boolean) {
        val initialHour = if (isStartTime) currentStartHour else currentEndHour
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, _ ->
            val tempStart = if (isStartTime) hourOfDay else currentStartHour
            val tempEnd = if (isStartTime) currentEndHour else hourOfDay

            if (tempStart == tempEnd) {
                Toast.makeText(this, "시작 시간과 종료 시간은 같을 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@OnTimeSetListener
            }

            // 시간 업데이트 (UI 반영은 updateTimeTextViews에서)
            currentStartHour = tempStart
            currentEndHour = tempEnd
            updateTimeTextViews() // UI 즉시 업데이트
            // ★★★ 시간 저장은 '설정 저장' 버튼 클릭 시 수행 ★★★
            // NotificationSettingsManager.saveNotificationTime(this, currentStartHour, currentEndHour)
        }

        val timePickerDialog = TimePickerDialog(
            this, timeSetListener, initialHour, 0, true
        )
        timePickerDialog.show()
    }

    // 입력된 키워드 추가
    private fun addKeywordFromInput() {
        val keyword = etKeyword.text.toString().trim()
        val currentKeywords = NotificationSettingsManager.getKeywords(this)

        if (keyword.isNotEmpty()) {
            if (currentKeywords.size >= 10) {
                Toast.makeText(this, "키워드는 최대 10개까지 등록할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            if (currentKeywords.contains(keyword)) {
                Toast.makeText(this, "이미 등록된 키워드입니다.", Toast.LENGTH_SHORT).show()
                return
            }
            // 알림 권한 확인 (필수는 아님, 스위치 켤 때 확인하므로)
            // if (!checkNotificationPermission()) { requestNotificationPermission(); return }

            // ★★★ 수정: NotificationSettingsManager.addKeyword 호출 (내부 저장만) ★★★
            NotificationSettingsManager.addKeyword(this, keyword)

            // UI 업데이트
            addChipToGroup(keyword)
            etKeyword.text.clear()
            updateKeywordCount()

            // ★★★ 여기서 Worker 상태 바로 변경 안 함 ★★★
            Log.d("NotiSettingsActivity", "Keyword '$keyword' added to UI.")
        }
    }

    // ChipGroup에 칩 추가 및 삭제 리스너
    private fun addChipToGroup(keyword: String) {
        try {
            val chip = LayoutInflater.from(this).inflate(R.layout.chip_keyword, chipGroupKeywords, false) as Chip
            chip.text = keyword
            chip.setOnCloseIconClickListener {
                // ★★★ 수정: NotificationSettingsManager.removeKeyword 호출 (내부 저장만) ★★★
                NotificationSettingsManager.removeKeyword(this, keyword)
                binding.chipGroupKeywords.removeView(chip) // UI에서 즉시 제거
                updateKeywordCount()
                // ★★★ 여기서 Worker 상태 바로 변경 안 함 ★★★
                Log.d("NotiSettingsActivity", "Keyword '$keyword' removed from UI.")
            }
            chipGroupKeywords.addView(chip)
        } catch (e: Exception) {
            Log.e("NotificationSettings", "Error inflating chip_keyword.xml.", e)
            Toast.makeText(this, "칩 레이아웃 파일을 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
        }
    }

    // 키워드 개수 TextView 업데이트 (기존과 동일)
    private fun updateKeywordCount() {
        val count = NotificationSettingsManager.getKeywords(this).size
        tvKeywordCount.text = "$count/10 키워드" // 예시: 최대 10개
    }

    // 키워드 입력 영역 및 개별 칩 활성화/비활성화 (기존과 유사)
    private fun updateKeywordSectionEnabledState() {
        val masterEnabled = switchMasterNotification.isChecked
        val keywordEnabled = switchKeywordNotification.isChecked
        val overallEnabled = masterEnabled && keywordEnabled // 둘 다 켜져야 활성화

        // 키워드 알림 스위치는 마스터 스위치가 켜져 있을 때만 활성화 가능
        switchKeywordNotification.isEnabled = masterEnabled

        // 키워드 입력 관련 UI는 overallEnabled 상태에 따라 활성화/비활성화
        etKeyword.isEnabled = overallEnabled
        btnAddKeyword.isEnabled = overallEnabled
        chipGroupKeywords.isEnabled = overallEnabled // 그룹 자체 활성화 (선택적)

        // 그룹 내 개별 칩들의 상호작용(닫기 버튼 등) 활성화/비활성화
        for (i in 0 until chipGroupKeywords.childCount) {
            chipGroupKeywords.getChildAt(i)?.apply {
                isEnabled = overallEnabled // 칩 자체 활성화
                // isCloseIconVisible = overallEnabled // 닫기 아이콘 보이기/숨기기 (선택적)
            }
        }
        Log.d("NotiSettingsActivity", "Keyword section enabled state updated: $overallEnabled (Master: $masterEnabled, Keyword: $keywordEnabled)")
    }

    // --- 권한 관련 함수 (기존과 동일) ---
    private fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true // TIRAMISU 미만 버전은 항상 true
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    private fun checkAndRequestNotificationPermission() {
        // 앱 시작 시 권한 없으면 요청 (선택적)
        // if (!checkNotificationPermission()) {
        //     requestNotificationPermission()
        // }
        // 또는 스위치 켤 때만 요청 (현재 로직)
    }
}

