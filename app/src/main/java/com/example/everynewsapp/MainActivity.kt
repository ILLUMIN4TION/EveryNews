package com.example.everynewsapp

import android.content.Intent // ★★★ Intent import 추가 ★★★
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
// import androidx.appcompat.widget.SearchView // ★★★ SearchView 관련 import 제거 ★★★
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
    // private var searchView: SearchView? = null // ★★★ 제거 ★★★

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

        // observeViewModel() // ★★★ 관련 옵저버가 없으므로 호출 제거 (또는 빈 함수로 둬도 됨) ★★★
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)

        searchMenuItem = menu?.findItem(R.id.action_search)

        // ★★★ START: SearchView 관련 리스너 모두 제거 ★★★
        // searchView = searchMenuItem?.actionView as? SearchView
        // searchView?.queryHint = getString(R.string.search_hint)
        // ... setOnQueryTextListener ...
        // ★★★ END: SearchView 관련 리스너 모두 제거 ★★★

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // ★★★ START: 검색 아이콘 클릭 시 SearchActivity 실행 ★★★
            R.id.action_search -> {
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            // ★★★ END: 검색 아이콘 클릭 시 SearchActivity 실행 ★★★
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ★★★ 이 함수는 이제 필요 없습니다. (NewsViewModel에서 관련 LiveData 제거됨) ★★★
    // private fun observeViewModel() {
    //     newsViewModel.collapseSearchViewEvent.observe(this) { ... }
    // }

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

        // 프래그먼트 종류에 따라 검색 아이콘 가시성 제어 (이 로직은 유효함)
        if (fragment is HomeFragment) {
            searchMenuItem?.isVisible = true
            Log.d("MainActivity", "Showing Search Icon for HomeFragment")
        } else {
            searchMenuItem?.isVisible = false
            Log.d("MainActivity", "Hiding Search Icon")

            // ★★★ SearchView가 닫히는 로직은 제거 (필요 없음) ★★★
            // if (searchMenuItem?.isActionViewExpanded == true) { ... }
        }
    }
}