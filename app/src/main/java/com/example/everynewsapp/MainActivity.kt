package com.example.everynewsapp

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView // SearchView 임포트
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

    // SearchView와 MenuItem 참조 변수
    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this) // 테마 적용
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar 설정
        setSupportActionBar(binding.toolbar)

        // ViewModel 초기화
        val database = AppDatabase.getDatabase(applicationContext)
        val newsDao = database.scrappedNewsDao()
        val newsRepository = NewsRepository(newsDao)
        val viewModelFactory = NewsViewModelFactory(newsRepository, application)
        newsViewModel = ViewModelProvider(this, viewModelFactory).get(NewsViewModel::class.java)

        // BottomNavigationView 설정
        setupBottomNavigationView()
        // 앱 최초 실행 시 HomeFragment 로드
        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
            replaceFragment(HomeFragment(), getString(R.string.app_title)) // ★★★ 초기 프래그먼트 설정 시에도 replaceFragment 호출 ★★★
        }

        // ViewModel 관찰 설정
        observeViewModel()
    }

    // Toolbar 메뉴 생성 (SearchView 설정 포함)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)

        // SearchView 설정
        searchMenuItem = menu?.findItem(R.id.action_search)
        searchView = searchMenuItem?.actionView as? SearchView

        searchView?.queryHint = getString(R.string.search_hint)
        searchView?.isSubmitButtonEnabled = true // 검색 버튼 활성화 (선택 사항)

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // 검색 버튼 클릭 시
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    Log.d("MainActivity", "Search Query Submitted: $query")
                    newsViewModel.searchNewsByCategory(query) // ViewModel에 검색 요청
                    newsViewModel.clearChipSelectionEvent.postValue(true) // HomeFragment 탭 해제 요청
                    searchMenuItem?.collapseActionView() // SearchView 닫기
                    searchView?.clearFocus() // 키보드 숨기기 (포커스 제거)
                }
                return true // 이벤트 소비됨
            }
            // 검색어 변경 시 (사용 안 함)
            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        // ★★★ 중요: 메뉴가 처음 생성될 때 replaceFragment가 이미 호출되어
        // ★★★ searchMenuItem의 가시성을 설정했을 수 있으므로, 여기서 강제로 숨기지 않습니다.
        // searchMenuItem?.isVisible = false // <-- 이 줄 제거 또는 주석 처리

        // ★★★ 추가: 현재 프래그먼트 확인 후 가시성 재설정 (안전 장치) ★★★
        val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
        searchMenuItem?.isVisible = currentFragment is HomeFragment

        return true
    }

    // Toolbar 메뉴 아이템 클릭 처리 (SearchView 외 다른 아이템용)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // R.id.action_search는 SearchView가 처리하므로 여기서 제외
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ViewModel 관찰 (SearchView 닫기 요청 처리)
    private fun observeViewModel() {
        newsViewModel.collapseSearchViewEvent.observe(this) { shouldCollapse ->
            if (shouldCollapse == true) {
                searchMenuItem?.collapseActionView() // SearchView 닫기
                searchView?.setQuery("", false) // 검색창 텍스트 비우기 (선택 사항)
                searchView?.clearFocus()
                newsViewModel.collapseSearchViewEvent.postValue(false) // 이벤트 소비
            }
        }
    }

    // BottomNavigationView 설정
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

    // ★★★ 프래그먼트 교체 및 검색 아이콘 가시성 제어 함수 ★★★
    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()

        // 1. 타이틀 변경
        supportActionBar?.title = title

        // 2. ★★★ 프래그먼트 종류에 따라 검색 아이콘 가시성 제어 ★★★
        // (onCreateOptionsMenu 호출 전/후 모두 대응 가능하도록 invalidateOptionsMenu 사용 고려)
        // invalidateOptionsMenu() // 메뉴를 강제로 다시 그리게 하여 onCreateOptionsMenu 재호출 유도

        // ★★★ 직접 가시성 제어 (더 간단한 방법) ★★★
        searchMenuItem?.let { // searchMenuItem이 null이 아닐 때만 실행
            if (fragment is HomeFragment) {
                it.isVisible = true // HomeFragment일 때 보이기
                Log.d("MainActivity", "Showing Search Icon for HomeFragment")
            } else {
                it.isVisible = false // 다른 프래그먼트일 때 숨기기
                Log.d("MainActivity", "Hiding Search Icon")
                // 검색창이 열려있었다면 닫기
                if (it.isActionViewExpanded) {
                    it.collapseActionView()
                }
            }
        } ?: run {
            // searchMenuItem이 아직 null인 경우 (앱 초기 실행 시점)
            // onCreateOptionsMenu에서 가시성을 처리하므로 여기서 할 일 없음
            Log.d("MainActivity", "searchMenuItem is null during replaceFragment (initial load likely)")
        }
    }
}

