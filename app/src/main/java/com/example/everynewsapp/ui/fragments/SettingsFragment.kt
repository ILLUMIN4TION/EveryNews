package com.example.everynewsapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.everynewsapp.ThemeManager
import com.example.everynewsapp.ThemeSettingsActivity
import com.example.everynewsapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "NewsAppPrefs"
    private val LOCK_SCREEN_KEY = "LockScreenEnabled"

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
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLockScreenEnabled = prefs.getBoolean(LOCK_SCREEN_KEY, false)
        binding.switchLockscreen.isChecked = isLockScreenEnabled
    }

    private fun setupEventListeners() {
        // 테마 설정 항목 클릭 리스너
        binding.llThemeSetting.setOnClickListener {
            val intent = Intent(requireActivity(), ThemeSettingsActivity::class.java)
            startActivity(intent)
        }

        // 잠금화면 뉴스 표시 토글 리스너
        binding.switchLockscreen.setOnCheckedChangeListener { _, isChecked ->
            val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(LOCK_SCREEN_KEY, isChecked).apply()

            if (isChecked) {
                // 서비스 시작
                val serviceIntent = Intent(requireActivity(), LockScreenService::class.java)
                requireActivity().startService(serviceIntent)
                Toast.makeText(context, "잠금화면 뉴스 표시가 활성화되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 서비스 중지
                val serviceIntent = Intent(requireActivity(), LockScreenService::class.java)
                requireActivity().stopService(serviceIntent)
                Toast.makeText(context, "잠금화면 뉴스 표시가 비활성화되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 테마 설정에서 돌아왔을 때 현재 테마가 적용되도록 재설정
        ThemeManager.applyTheme(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
