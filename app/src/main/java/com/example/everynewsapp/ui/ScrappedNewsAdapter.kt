package com.example.everynewsapp.ui

import android.content.Intent
import android.os.Build
import android.text.Html
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
import com.example.everynewsapp.news.model.toNewsItem
import com.example.everynewsapp.news.viewModel.NewsViewModel

class ScrappedNewsAdapter(
    private val newsList: MutableList<ScrappedNewsItem>,
    private val newsViewModel: NewsViewModel
) : RecyclerView.Adapter<ScrappedNewsAdapter.ScrappedNewsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrappedNewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return ScrappedNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScrappedNewsViewHolder, position: Int) {
        val item = newsList[position]

        holder.title.text = item.title.stripHtml()
        holder.desc.text = item.description?.stripHtml()

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
            // ★★★ 핵심: NewsViewModel의 removeScrap 함수를 직접 호출합니다. ★★★
            newsViewModel.removeScrap(item)
            // 삭제가 성공하면 LiveData를 통해 자동으로 목록이 갱신되어 사라집니다.
            Toast.makeText(holder.itemView.context, "스크랩 해제됨", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = newsList.size

    fun updateData(newNewsList: List<ScrappedNewsItem>) {
        newsList.clear()
        newsList.addAll(newNewsList)
        notifyDataSetChanged()
    }

    private fun String?.stripHtml(): String {
        if (this == null) return ""
        return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
    }

    class ScrappedNewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_default_news_title)
        val desc: TextView = view.findViewById(R.id.tv_default_news_description)
        val thumbnail: ImageView = view.findViewById(R.id.iv_defaul_news_thumbnail)
        val scrapButton: ImageView = view.findViewById(R.id.imv_news_scrap)
    }
}