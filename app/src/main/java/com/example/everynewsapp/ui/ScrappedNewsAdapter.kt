package com.example.everynewsapp.ui

import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.everynewsapp.NewsDetailActivity
import com.example.everynewsapp.R
import com.example.everynewsapp.news.model.ScrappedNewsItem
import android.text.Html
import com.example.everynewsapp.news.viewModel.NewsDetailViewModel
import com.example.everynewsapp.news.viewModel.ScrappedNewsViewModel

class ScrappedNewsAdapter(private var newsList: List<ScrappedNewsItem> = emptyList(), private val viewModel: ScrappedNewsViewModel) :
    RecyclerView.Adapter<ScrappedNewsAdapter.ScrappedNewsViewHolder>() {

    class ScrappedNewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_default_news_title)
        val desc: TextView = view.findViewById(R.id.tv_default_news_description)
        val thumbnail: ImageView = view.findViewById(R.id.iv_defaul_news_thumbnail)
        val scrapButton: ImageView = view.findViewById(R.id.imv_news_scrap)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrappedNewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return ScrappedNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScrappedNewsViewHolder, position: Int) {
        val item = newsList[position]

        // ★★★ HTML 태그를 제거하고 바로 TextView에 설정 ★★★
        holder.title.text = item.title.stripHtml()
        holder.desc.text = item.description?.stripHtml()

        // 썸네일 이미지 로드
        if (!item.imageUrl.isNullOrEmpty()) {
            holder.thumbnail.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_search)
                error(R.drawable.ic_search)
            }
        } else {
            holder.thumbnail.load(R.drawable.ic_search)
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, NewsDetailActivity::class.java).apply {
                putExtra("NEWS_ITEM", item)
            }
            Log.e("NewsDetailActivity", "NEWS_ITEM: $item")
            context.startActivity(intent)
        }

        holder.scrapButton.setOnClickListener {
            viewModel.toggleScrap(item)
            Toast.makeText(holder.itemView.context, "스크랩 상태 변경", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = newsList.size

    fun updateData(newNewsList: List<ScrappedNewsItem>) {
        newsList = newNewsList
        notifyDataSetChanged()
    }
}

// String에 HTML 태그를 제거하는 확장 함수
private fun String.stripHtml(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(this).toString()
    }
}