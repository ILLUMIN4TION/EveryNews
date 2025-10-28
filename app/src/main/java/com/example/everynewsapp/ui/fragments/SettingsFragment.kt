package com.example.everynewsapp.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.everynewsapp.ThemeManager
import com.example.everynewsapp.ThemeSettingsActivity
import com.example.everynewsapp.databinding.FragmentSettingsBinding
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val lockScreenWorkTag = "lockScreenNewsUpdate"
    private val PREFS_NAME = "NewsAppPrefs"
    private val LOCK_SCREEN_KEY = "LockScreenEnabled"

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 권한 허용 시 서비스 및 워커 등록
                startLockScreenFeatures()
            } else {
                // 권한 거부 시, 스위치를 다시 끄고 토스트 메시지 표시
                Toast.makeText(requireContext(), "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                binding.switchLockscreen.isChecked = false
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SharedPreferences 대신 isServiceRunning으로 상태 로드
        setupLockscreenSwitch()
        setupEventListeners()
    }

    private fun setupLockscreenSwitch() {
        // SharedPreferences 대신, 실제 서비스 실행 여부로 스위치 상태 설정
        binding.switchLockscreen.isChecked = isServiceRunning(LockScreenService::class.java)

        binding.switchLockscreen.setOnCheckedChangeListener { _, isChecked ->
            // SharedPreferences 상태 업데이트
            val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(LOCK_SCREEN_KEY, isChecked).apply()

            if (isChecked) {
                checkNotificationPermissionAndStartFeatures()
            } else {
                stopLockScreenFeatures()
            }
        }
    }


    private fun setupEventListeners() {
        // 테마 설정 항목 클릭 리스너 (기존과 동일)
        binding.llThemeSetting.setOnClickListener {
            val intent = Intent(requireActivity(), ThemeSettingsActivity::class.java)
            startActivity(intent)
        }

        // TODO: NotificationSettingsActivity 관련 이벤트가 있었다면 여기에 추가
        // binding.llNotificationSetting.setOnClickListener { ... }
    }

    // ★★★ 서비스 시작 전 알림 권한 확인 로직 ★★★
    private fun checkNotificationPermissionAndStartFeatures() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startLockScreenFeatures() // 권한 있을 경우 바로 실행
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) // 권한 요청
                }
            }
        } else {
            startLockScreenFeatures() // Android 12 이하에서는 권한 없이 바로 실행
        }
    }

    // ★★★ 서비스 및 워커 시작 (통합) ★★★
    private fun startLockScreenFeatures() {
        startLockScreenService()
        scheduleNewsUpdateWorker() // 워커 등록
        Toast.makeText(context, "잠금화면 뉴스 표시가 활성화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    // ★★★ 서비스 및 워커 중지 (통합) ★★★
    private fun stopLockScreenFeatures() {
        stopLockScreenService()
        cancelNewsUpdateWorker() // 워커 취소
        Toast.makeText(context, "잠금화면 뉴스 표시가 비활성화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun startLockScreenService() {
        val serviceIntent = Intent(requireActivity(), LockScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(serviceIntent)
        } else {
            requireActivity().startService(serviceIntent)
        }
    }

    private fun stopLockScreenService() {
        requireActivity().stopService(Intent(requireActivity(), LockScreenService::class.java))
    }

    // ★★★ 워커 등록 로직 (15분 주기 유지) ★★★
    private fun scheduleNewsUpdateWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val newsUpdateWorkRequest = PeriodicWorkRequest.Builder(
            NewsUpdateWorker::class.java,
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(lockScreenWorkTag)
            .build()

        // WorkManager에 유일한 이름으로 등록하고, 기존 작업이 있다면 업데이트합니다.
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            lockScreenWorkTag,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            newsUpdateWorkRequest
        )
        Log.d("SettingsActivity", "NewsUpdateWorker가 15분 주기로 Unique 등록/업데이트되었습니다.")
    }

    private fun cancelNewsUpdateWorker() {
        WorkManager.getInstance(requireContext()).cancelAllWorkByTag(lockScreenWorkTag)
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyTheme(requireActivity())
        // 스위치 상태를 서비스 실행 여부로 다시 확인 (최신 상태 동기화)
        binding.switchLockscreen.isChecked = isServiceRunning(LockScreenService::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}