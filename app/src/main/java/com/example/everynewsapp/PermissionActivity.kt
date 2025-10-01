package com.example.everynewsapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.everynewsapp.ui.LockScreenService

class PermissionActivity : AppCompatActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                checkOverlayPermission()
            } else {
                Toast.makeText(this, "알림 권한을 허용해야 앱을 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }
        }

    private val requestOverlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startMainActivity()
            } else {
                Toast.makeText(this, "다른 앱 위에 표시 권한을 허용해야 앱을 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                checkOverlayPermission()
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                startMainActivity()
            } else {
                Toast.makeText(
                    this,
                    "다음 화면에서 'EveryNews 앱'을 찾아 '다른 앱 위에 표시' 권한을 허용해 주세요.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                requestOverlayPermissionLauncher.launch(intent)
            }
        } else {
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        // --- 이 부분이 수정되었습니다 ---
        // '첫 실행'인지 확인하는 플래그를 사용합니다.
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRun", true)

        if (isFirstRun) {
            // 첫 실행이라면, 락 스크린 서비스를 자동으로 시작합니다.
            startLockScreenService()
            // 다음부터는 첫 실행이 아님을 기록합니다.
            prefs.edit().putBoolean("isFirstRun", false).apply()
        }
        // --- 여기까지 ---

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // 서비스 시작 로직을 이 Activity에도 추가합니다.
    private fun startLockScreenService() {
        val serviceIntent = Intent(this, LockScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}