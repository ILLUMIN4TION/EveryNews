package com.example.everynewsapp

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.everynewsapp.databinding.ActivitySettingsBinding
import com.example.everynewsapp.ui.LockScreenService

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    // 1. 권한 요청 결과를 처리하는 '런처'를 선언합니다.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 사용자가 권한을 허용한 경우, 서비스를 시작합니다.
                startLockScreenService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.llThemeSetting.setOnClickListener {
            startActivity(Intent(this, ThemeSettingsActivity::class.java))
        }
        binding.llNotificationSetting.setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        setupLockscreenSwitch()
        setupBottomNav()
    }

    private fun setupLockscreenSwitch() {
        binding.switchLockscreen.isChecked = isServiceRunning(LockScreenService::class.java)

        binding.switchLockscreen.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 2. 스위치를 켤 때, 바로 서비스를 시작하는 대신 권한을 먼저 확인합니다.
                checkNotificationPermissionAndStartService()
            } else {
                // 스위치를 끄면 서비스 중지
                stopService(Intent(this, LockScreenService::class.java))
            }
        }
    }

    // 3. 알림 권한을 확인하고, 없으면 요청, 있으면 서비스를 시작하는 함수
    private fun checkNotificationPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상에서만
            when {
                ContextCompat.checkSelfPermission(
                    this,
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

    // 4. 서비스 시작 로직을 별도 함수로 분리
    private fun startLockScreenService() {
        val serviceIntent = Intent(this, LockScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun setupBottomNav() {
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
                R.id.navigation_settings -> true
                else -> false
            }
        }
    }
}