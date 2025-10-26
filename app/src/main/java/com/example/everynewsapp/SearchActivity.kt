//package com.example.everynewsapp
//
//import android.os.Bundle
//import android.view.View
//import android.view.inputmethod.EditorInfo
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.ViewModelProvider
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.everynewsapp.databinding.ActivitySearchBinding
//import com.example.everynewsapp.news.database.AppDatabase
//import com.example.everynewsapp.news.repository.NewsRepository
//import com.example.everynewsapp.news.viewModel.NewsViewModel
//import com.example.everynewsapp.news.viewModel.NewsViewModelFactory
//import com.example.everynewsapp.ui.NewsAdapter
//
//class SearchActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivitySearchBinding
//    private lateinit var newsViewModel: NewsViewModel
//    private lateinit var newsAdapter: NewsAdapter
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivitySearchBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        val database = AppDatabase.getDatabase(applicationContext)
//        val newsRepository = NewsRepository(database.scrappedNewsDao())
//        val viewModelFactory = NewsViewModelFactory(newsRepository, application)
//        newsViewModel = ViewModelProvider(this, viewModelFactory)[NewsViewModel::class.java]
//
//        setupRecyclerView()
//        setupSearch()
//        observeViewModel()
//    }
//
//    private fun setupRecyclerView() {
//        newsAdapter = NewsAdapter(mutableListOf()) { newsItem ->
//            newsViewModel.toggleScrap(newsItem)
//        }
//        binding.rvSearchNews.adapter = newsAdapter
//        binding.rvSearchNews.layoutManager = LinearLayoutManager(this)
//
//        binding.rvSearchNews.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
//                val totalItemCount = layoutManager.itemCount
//                val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()
//
//                if (!newsViewModel.isLoadInProgress.value!! && totalItemCount <= (lastVisibleItem + 2)) {
//                    newsViewModel.loadMoreSearchNews()
//                }
//            }
//        })
//    }
//
//    private fun setupSearch() {
//        binding.etSearch.setOnEditorActionListener { textView, actionId, _ ->
//            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
//                val query = textView.text.toString()
//                newsViewModel.searchNews(query)
//                true
//            } else {
//                false
//            }
//        }
//    }
//
//    private fun observeViewModel() {
//        newsViewModel.searchNewsList.observe(this) { news ->
//            newsAdapter.updateData(news)
//            binding.tvNoResults.visibility = if (news.isEmpty()) View.VISIBLE else View.GONE
//        }
//
//        newsViewModel.searchLoadMoreEvent.observe(this) { news ->
//            newsAdapter.addData(news)
//        }
//
//        newsViewModel.isLoadInProgress.observe(this) { isLoading ->
//            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
//        }
//    }
//}