package com.example.everynewsapp.ui

import android.content.Intent
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.everynewsapp.NewsDetailActivity
import com.example.everynewsapp.R
import com.example.everynewsapp.databinding.ItemNewsBinding
import com.example.everynewsapp.news.model.NewsItem

class NewsAdapter(
    private val newsList: MutableList<NewsItem>,
    private val onScrapClick: (NewsItem) -> Unit
) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(newsList[position], onScrapClick)
    }

    override fun getItemCount(): Int = newsList.size



    fun updateData(newNewsList: List<NewsItem>) {
        newsList.clear()
        newsList.addAll(newNewsList)
        notifyDataSetChanged()
    }

    // ★★★ 무한 스크롤 시 새 데이터를 추가만 함 (loadMore 시 사용) ★★★
    fun addData(newNewsList: List<NewsItem>) {
        val startPosition = newsList.size
        newsList.addAll(newNewsList)
        notifyItemRangeInserted(startPosition, newNewsList.size)
    }

    class ViewHolder(private val binding: ItemNewsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NewsItem, onScrapClick: (NewsItem) -> Unit) {
            binding.tvDefaultNewsTitle.text = item.title.stripHtml()
            binding.tvDefaultNewsDescription.text = item.description.stripHtml()
            binding.ivDefaulNewsThumbnail.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_search)
                error(R.drawable.ic_search)
            }

            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, NewsDetailActivity::class.java).apply {
                    putExtra("NEWS_ITEM", item)
                }
                context.startActivity(intent)
            }

            binding.imvNewsScrap.setOnClickListener {
                onScrapClick(item)
                Toast.makeText(itemView.context, "스크랩!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun String?.stripHtml(): String {
    if (this == null) return ""
    return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
}