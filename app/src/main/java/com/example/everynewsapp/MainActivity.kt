package com.example.everynewsapp

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)
        val viewModelFactory = NewsViewModelFactory(newsRepository, application)
        newsViewModel = ViewModelProvider(this, viewModelFactory).get(NewsViewModel::class.java)

        setupBottomNavigationView()
        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
            replaceFragment(HomeFragment(), getString(R.string.app_title))
        }

        observeViewModel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)

        searchMenuItem = menu?.findItem(R.id.action_search)
        searchView = searchMenuItem?.actionView as? SearchView

        searchView?.queryHint = getString(R.string.search_hint)
        searchView?.isSubmitButtonEnabled = true

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    Log.d("MainActivity", "Search Query Submitted: $query")
                    newsViewModel.searchNewsByCategory(query)
                    newsViewModel.clearChipSelectionEvent.postValue(true)
                    searchMenuItem?.collapseActionView()
                    searchView?.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        // ★★★ 수정: 이 줄을 삭제합니다. ★★★
        // (replaceFragment 함수가 가시성을 제어하도록 둡니다.)
        // searchMenuItem?.isVisible = false

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeViewModel() {
        newsViewModel.collapseSearchViewEvent.observe(this) { shouldCollapse ->
            if (shouldCollapse == true) {
                searchMenuItem?.collapseActionView()
                searchView?.clearFocus()
                newsViewModel.collapseSearchViewEvent.postValue(false)
            }
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

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ★★★ 핵심 수정: replaceFragment 함수 ★★★
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()

        // 1. 타이틀 변경 (기존과 동일)
        supportActionBar?.title = title

        // 2. ★★★ 프래그먼트 종류에 따라 검색 아이콘 가시성 제어 ★★★
        // (searchMenuItem이 null이 아닐 때만 실행되도록 ?. 사용)
        if (fragment is HomeFragment) {
            // HomeFragment일 때만 검색 아이콘을 보이게 합니다.
            searchMenuItem?.isVisible = true
            Log.d("MainActivity", "Showing Search Icon for HomeFragment")
        } else {
            // 다른 모든 프래그먼트에서는 검색 아이콘을 숨깁니다.
            searchMenuItem?.isVisible = false
            Log.d("MainActivity", "Hiding Search Icon")

            // ★★★ (중요) 혹시 검색창이 열려있는 상태로 다른 탭으로 이동하면 닫아줍니다. ★★★
            if (searchMenuItem?.isActionViewExpanded == true) {
                searchMenuItem?.collapseActionView()
            }
        }
    }
}

