package com.example.everynewsapp.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.everynewsapp.NotificationSettingsActivity // ★★★ 추가 ★★★
import com.example.everynewsapp.R // (R이 필요할 수 있으므로 추가)
import com.example.everynewsapp.ThemeManager
import com.example.everynewsapp.ThemeSettingsActivity
import com.example.everynewsapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // --- SharedPreferences 로직 제거 ---
    // (isServiceRunning으로 대체)
    // private val PREFS_NAME = "NewsAppPrefs"
    // private val LOCK_SCREEN_KEY = "LockScreenEnabled"

    // ★★★ 권한 요청 결과를 처리하는 '런처' 선언 (Activity 로직 가져오기) ★★★
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 사용자가 권한을 허용한 경우, 서비스를 시작합니다.
                startLockScreenService()
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

        // --- loadSettings() 제거 ---
        setupEventListeners()
        setupLockscreenSwitch() // ★★★ Activity 로직을 가져온 함수 호출 ★★★
    }

    // --- loadSettings() 함수 제거 ---

    private fun setupEventListeners() {
        // 테마 설정 항목 클릭 리스너 (기존과 동일)
        binding.llThemeSetting.setOnClickListener {
            val intent = Intent(requireActivity(), ThemeSettingsActivity::class.java)
            startActivity(intent)
        }

        // ★★★ 알림 설정 항목 클릭 리스너 (Activity 로직에서 추가) ★★★
        binding.llNotificationSetting.setOnClickListener {
            startActivity(Intent(requireActivity(), NotificationSettingsActivity::class.java))
        }

        // --- 스위치 리스너 로직은 setupLockscreenSwitch()로 이동 ---
    }

    // ★★★ Activity의 setupLockscreenSwitch 로직을 프래그먼트용으로 수정 ★★★
    private fun setupLockscreenSwitch() {
        // SharedPreferences 대신, 실제 서비스 실행 여부로 스위치 상태 설정
        binding.switchLockscreen.isChecked = isServiceRunning(LockScreenService::class.java)

        binding.switchLockscreen.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 스위치를 켤 때, 바로 서비스를 시작하는 대신 권한을 먼저 확인
                checkNotificationPermissionAndStartService()
            } else {
                // 스위치를 끄면 서비스 중지
                val serviceIntent = Intent(requireActivity(), LockScreenService::class.java)
                requireActivity().stopService(serviceIntent)
                Toast.makeText(context, "잠금화면 뉴스 표시가 비활성화되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ★★★ 알림 권한을 확인하고, 없으면 요청, 있으면 서비스를 시작하는 함수 ★★★
    private fun checkNotificationPermissionAndStartService() {
        // Android 13 (TIRAMISU) 이상에서만 알림 권한 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 이미 권한이 있는 경우, 서비스 시작
                    startLockScreenService()
                }
                else -> {
                    // 권한이 없는 경우, 권한 요청 팝업을 띄웁니다.
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 이하 버전에서는 권한이 필요 없으므로 바로 서비스 시작
            startLockScreenService()
        }
    }

    // ★★★ 서비스 시작 로직을 별도 함수로 분리 (Activity 로직 가져오기) ★★★
    private fun startLockScreenService() {
        val serviceIntent = Intent(requireActivity(), LockScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(serviceIntent)
        } else {
            requireActivity().startService(serviceIntent)
        }
        Toast.makeText(context, "잠금화면 뉴스 표시가 활성화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    // ★★★ 서비스 실행 여부 확인 함수 (Activity 로직 가져오기) ★★★
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // getRunningServices는 API 30+에서 제한되지만, 사용자의 원본 코드를 유지합니다.
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    override fun onResume() {
        super.onResume()
        // 테마 설정에서 돌아왔을 때 현재 테마가 적용되도록 재설정
        ThemeManager.applyTheme(requireActivity())

        // ★★★ 추가: 다른 화면에서 돌아왔을 때 스위치 상태 갱신 ★★★
        // (예: 앱 설정에서 직접 권한을 끄고 돌아온 경우)
        binding.switchLockscreen.isChecked = isServiceRunning(LockScreenService::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
