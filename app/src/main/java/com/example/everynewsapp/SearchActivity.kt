package com.example.everynewsapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.everynewsapp.databinding.ActivitySearchBinding
import com.example.everynewsapp.news.database.AppDatabase
import com.example.everynewsapp.news.repository.NewsRepository
import com.example.everynewsapp.news.viewModel.NewsViewModel
import com.example.everynewsapp.news.viewModel.NewsViewModelFactory
import com.example.everynewsapp.ui.NewsAdapter

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var newsViewModel: NewsViewModel
    private lateinit var newsAdapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 설정 (★★★ 바인딩 참조 수정 ★★★)
        setSupportActionBar(binding.toolbar) // 'appBarLayout' 제거
        supportActionBar?.setDisplayShowTitleEnabled(false) // 툴바 제목 숨김
        binding.toolbar.setNavigationOnClickListener { // 'appBarLayout' 제거
            onBackPressedDispatcher.onBackPressed() // 뒤로가기 버튼 클릭 시
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val newsRepository = NewsRepository(database.scrappedNewsDao())
        val viewModelFactory = NewsViewModelFactory(newsRepository, application)
        newsViewModel = ViewModelProvider(this, viewModelFactory)[NewsViewModel::class.java]

        setupRecyclerView()
        setupSearchEditText()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(mutableListOf()) { newsItem ->
            newsViewModel.toggleScrap(newsItem)
        }
        binding.rvSearchNews.adapter = newsAdapter
        binding.rvSearchNews.layoutManager = LinearLayoutManager(this)

        binding.rvSearchNews.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()

                if (newsViewModel.isLoadInProgress.value != true && totalItemCount <= (lastVisibleItem + 2)) {
                    newsViewModel.loadMoreSearchNews()
                }
            }
        })
    }

    private fun setupSearchEditText() {
        // 키보드의 '검색' 버튼(actionSearch) 리스너
        binding.etSearch.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = textView.text.toString()
                if (query.isNotBlank()) {
                    newsViewModel.searchNews(query)
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(textView.windowToken, 0)
                }
                true
            } else {
                false
            }
        }

        // 텍스트 변경 감지 (닫기 버튼 표시/숨김)
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 텍스트가 있으면 닫기 아이콘 표시, 없으면 숨김
                binding.etSearch.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_search_gray, 0,
                    if (s?.isNotEmpty() == true) R.drawable.ic_clear_gray else 0,
                    0
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 닫기 아이콘 클릭 리스너
        binding.etSearch.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // 닫기 아이콘이 보이는 영역을 클릭했는지 확인
                val drawableEnd = binding.etSearch.compoundDrawablesRelative[2] // 닫기 아이콘 (drawableEnd)
                if (drawableEnd != null && event.rawX >= (binding.etSearch.right - drawableEnd.bounds.width() - binding.etSearch.paddingEnd)) {
                    binding.etSearch.text.clear() // 텍스트 지우기
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun observeViewModel() {
        newsViewModel.searchNewsList.observe(this) { news ->
            newsAdapter.updateData(news)
            binding.tvNoResults.visibility = if (news.isEmpty()) View.VISIBLE else View.GONE
        }

        newsViewModel.searchLoadMoreEvent.observe(this) { news ->
            newsAdapter.addData(news)
        }

        newsViewModel.isLoadInProgress.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}