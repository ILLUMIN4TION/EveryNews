package com.example.everynewsapp.news.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.everynewsapp.R
import com.example.everynewsapp.news.model.NewsItem

class TrendingNewsAdapter(private val newsList: List<NewsItem>) :
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

        // 1. 뉴스 제목 표시 (HTML 태그 제거)
        holder.title.text = item.title.replace("<b>", "").replace("</b>", "")

        // 2. 썸네일 이미지 로드
        val imageUrl = item.imageUrl
        if (imageUrl != null) {
            // 크롤링된 이미지가 있는 경우, 해당 URL을 로드
            holder.thumbnail.load(imageUrl) {
                crossfade(true)
            }
        } else {
            // 크롤링에 실패했거나 이미지가 없는 경우, 기본 이미지를 로드
            holder.thumbnail.load(R.drawable.ic_search)
        }
    }

    override fun getItemCount() = newsList.size

    /**
     * RecyclerView 데이터를 업데이트하는 함수
     * 메인 액티비티에서 새로운 뉴스 목록을 가져올 때 호출
     */
    fun updateData(newNewsList: List<NewsItem>) {
        (newsList as ArrayList).clear()
        (newsList as ArrayList).addAll(newNewsList)
        notifyDataSetChanged()
    }
}