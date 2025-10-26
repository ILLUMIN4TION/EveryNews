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
                startLockScreenFeatures()
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

        loadSettings()
        setupEventListeners()
    }

    private fun loadSettings() {
        binding.switchLockscreen.isChecked = isServiceRunning(LockScreenService::class.java)
    }

    private fun setupEventListeners() {
        binding.llThemeSetting.setOnClickListener {
            val intent = Intent(requireActivity(), ThemeSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.switchLockscreen.setOnCheckedChangeListener { _, isChecked ->
            val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(LOCK_SCREEN_KEY, isChecked).apply()

            if (isChecked) {
                checkNotificationPermissionAndStartFeatures()
            } else {
                stopLockScreenFeatures()
            }
        }
    }

    private fun checkNotificationPermissionAndStartFeatures() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startLockScreenFeatures()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startLockScreenFeatures()
        }
    }

    private fun startLockScreenFeatures() {
        startLockScreenService()
        scheduleNewsUpdateWorker() // 워커 등록
        Toast.makeText(context, "잠금화면 뉴스 표시가 활성화되었습니다.", Toast.LENGTH_SHORT).show()
    }

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

    // ★★★ 워커 등록 로직 수정: 주기를 15분으로 변경 ★★★
    private fun scheduleNewsUpdateWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val newsUpdateWorkRequest = PeriodicWorkRequest.Builder(
            NewsUpdateWorker::class.java,
            // WorkManager 최소 주기인 15분으로 설정
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(lockScreenWorkTag)
            .build()

        // 기존 작업이 있다면 REPLACE
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            lockScreenWorkTag,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            newsUpdateWorkRequest
        )
        Log.d("SettingsActivity", "NewsUpdateWorker가 15분 주기로 등록되었습니다.")
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
        binding.switchLockscreen.isChecked = isServiceRunning(LockScreenService::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
