package com.example.everynewsapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everynewsapp.databinding.FragmentScrapBinding
import com.example.everynewsapp.news.viewModel.NewsViewModel

class ScrapFragment : Fragment() {

    private var _binding: FragmentScrapBinding? = null
    private val binding get() = _binding!!

    // MainActivity의 NewsViewModel 공유
    private val newsViewModel: NewsViewModel by activityViewModels()

    private lateinit var scrappedNewsAdapter: ScrappedNewsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScrapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        scrappedNewsAdapter = ScrappedNewsAdapter(mutableListOf(), newsViewModel)
        binding.rvScrappedNews.adapter = scrappedNewsAdapter
        binding.rvScrappedNews.layoutManager = LinearLayoutManager(context)
    }

    private fun observeViewModel() {
        newsViewModel.scrappedNewsList.observe(viewLifecycleOwner) { scrappedList ->
            scrappedNewsAdapter.updateData(scrappedList)
            updateEmptyState(scrappedList.isEmpty())
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.rvScrappedNews.visibility = View.GONE
            binding.tvEmptyScrap.visibility = View.VISIBLE
        } else {
            binding.rvScrappedNews.visibility = View.VISIBLE
            binding.tvEmptyScrap.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
