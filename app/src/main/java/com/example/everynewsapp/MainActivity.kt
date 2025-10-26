package com.example.everynewsapp

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView // ★★★ 추가 ★★★
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

    // ★★★ SearchView를 참조하기 위한 변수 추가 ★★★
    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

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

        // ★★★ HomeFragment에서 칩 클릭 시 SearchView를 닫기 위한 옵저버 추가 ★★★
        observeViewModel()
    }

    // ★★★ 앱 바 메뉴 추가 (SearchView 설정) ★★★
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)

        // ★★★ 검색 메뉴 아이템 및 SearchView 설정 ★★★
        searchMenuItem = menu?.findItem(R.id.action_search)
        searchView = searchMenuItem?.actionView as? SearchView

        searchView?.queryHint = getString(R.string.search_hint)
        searchView?.isSubmitButtonEnabled = true // (선택 사항: 제출 버튼 활성화)

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // 키보드의 '검색' 버튼을 눌렀을 때
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    Log.d("MainActivity", "Search Query Submitted: $query")
                    // 1. ViewModel으로 검색 실행
                    newsViewModel.searchNewsByCategory(query)
                    // 2. HomeFragment의 칩 선택 해제 요청
                    newsViewModel.clearChipSelectionEvent.postValue(true)
                    // 3. 검색창 닫고 포커스 제거
                    searchMenuItem?.collapseActionView()
                    searchView?.clearFocus()
                }
                return true
            }

            // 검색창 텍스트가 바뀔 때 (여기서는 사용 안 함)
            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        return true
    }

    // ★★★ 앱 바 아이템 클릭 이벤트 처리 ★★★
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // ★★★ SearchActivity로 이동하는 로직 삭제 ★★★
        // R.id.action_search 케이스는 이제 SearchView가 자동으로 처리합니다.
        return when (item.itemId) {
            /* (R.id.action_search 케이스 제거) */
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ★★★ ViewModel 옵저버 추가 (새 함수) ★★★
    private fun observeViewModel() {
        newsViewModel.collapseSearchViewEvent.observe(this) { shouldCollapse ->
            if (shouldCollapse == true) { // null 체크
                // HomeFragment에서 칩을 클릭하면 SearchView를 닫습니다.
                searchMenuItem?.collapseActionView()
                searchView?.clearFocus()
                newsViewModel.collapseSearchViewEvent.postValue(false) // 이벤트 소비
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

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
        supportActionBar?.title = title
    }
}
