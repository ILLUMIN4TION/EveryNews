package com.example.everynewsapp.ui

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.everynewsapp.R
import com.example.everynewsapp.news.model.NewsItem

class TrendingNewsAdapter(private var newsList: List<NewsItem>) : // ★ newsList를 val에서 var로 변경 ★
    RecyclerView.Adapter<TrendingNewsAdapter.TrendingNewsViewHolder>() {

    class TrendingNewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_small_news_title)
        val thumbnail: ImageView = view.findViewById(R.id.iv_small_news_thumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingNewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news_small, parent, false)
        return TrendingNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrendingNewsViewHolder, position: Int) {
        val item = newsList[position]

        // ★ HTML.fromHtml()을 사용하여 HTML 태그와 엔티티를 자동으로 변환 ★
        holder.title.text = Html.fromHtml(item.title, Html.FROM_HTML_MODE_LEGACY).toString()

        // 2. 썸네일 이미지 로드
        val imageUrl = item.imageUrl
        if (imageUrl != null) {
            holder.thumbnail.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_search)
                error(R.drawable.ic_search)
            }
        } else {
            holder.thumbnail.load(R.drawable.ic_search)
        }
    }

    override fun getItemCount() = newsList.size

    /**
     * RecyclerView 데이터를 업데이트하는 함수
     */
    fun updateData(newNewsList: List<NewsItem>) {
        newsList = newNewsList // ★ 리스트를 새로운 리스트로 재할당 ★
        notifyDataSetChanged()
    }
}