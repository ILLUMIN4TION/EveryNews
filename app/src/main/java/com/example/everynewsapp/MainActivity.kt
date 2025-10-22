package com.example.everynewsapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
    private lateinit var newsViewModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 앱 바 설정 (Activity가 Toolbar를 호스팅합니다)
        setSupportActionBar(binding.toolbar)

        // 2. 공통 ViewModel 초기화 (Activity Scope)
        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)
        val viewModelFactory = NewsViewModelFactory(newsRepository, application)
        newsViewModel = ViewModelProvider(this, viewModelFactory).get(NewsViewModel::class.java)

        setupBottomNavigationView()
        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
            // 초기 프래그먼트 로드 시 제목 설정 함수 호출
            replaceFragment(HomeFragment(), getString(R.string.app_title))
        }
    }

    // ★★★ 앱 바 메뉴 추가 (검색 아이콘) ★★★
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)
        return true
    }

    // ★★★ 앱 바 아이템 클릭 이벤트 처리 ★★★
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // 검색 아이콘 클릭 시 SearchActivity로 이동
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupBottomNavigationView() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment(), getString(R.string.app_title))
                    true
                }
                R.id.navigation_recommend -> {
                    replaceFragment(RecommendFragment(), getString(R.string.title_recommend))
                    true
                }
                R.id.navigation_scrap -> {
                    replaceFragment(ScrapFragment(), getString(R.string.title_scrap))
                    true
                }
                R.id.navigation_settings -> {
                    replaceFragment(SettingsFragment(), getString(R.string.title_settings))
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
        supportActionBar?.title = title
    }
}
