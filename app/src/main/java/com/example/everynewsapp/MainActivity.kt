package com.example.everynewsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.everynewsapp.databinding.ActivityMainBinding
import com.example.everynewsapp.news.database.AppDatabase
import com.example.everynewsapp.news.repository.NewsRepository
import com.example.everynewsapp.news.viewModel.NewsViewModel
import com.example.everynewsapp.news.viewModel.NewsViewModelFactory
import com.example.everynewsapp.ui.HomeFragment
import com.example.everynewsapp.ui.RecommendFragment
import com.example.everynewsapp.ui.ScrapFragment
import com.example.everynewsapp.ui.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var newsViewModel: NewsViewModel // 모든 프래그먼트가 공유할 뷰모델

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 공통 ViewModel 초기화 (Activity Scope)
        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)
        // Main Activity의 Lifecycle을 따르는 NewsViewModel을 준비합니다.
        val viewModelFactory = NewsViewModelFactory(newsRepository, application)
        newsViewModel = ViewModelProvider(this, viewModelFactory).get(NewsViewModel::class.java)

        setupBottomNavigationView()
        // 앱 시작 시 HomeFragment를 기본 화면으로 설정
        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
            replaceFragment(HomeFragment())
        }
    }

    private fun setupBottomNavigationView() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navigation_recommend -> {
                    replaceFragment(RecommendFragment())
                    true
                }
                R.id.navigation_scrap -> {
                    replaceFragment(ScrapFragment())
                    true
                }
                R.id.navigation_settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
}
