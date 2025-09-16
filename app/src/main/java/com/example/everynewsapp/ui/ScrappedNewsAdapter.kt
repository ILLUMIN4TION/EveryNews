package com.example.everynewsapp.ui

// com.example.everynewsapp.ui 패키지에 추가

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.everynewsapp.R
import com.example.everynewsapp.news.model.ScrappedNewsItem

class ScrappedNewsAdapter(private var newsList: List<ScrappedNewsItem> = emptyList()) :
    RecyclerView.Adapter<ScrappedNewsAdapter.ScrappedNewsViewHolder>() {

    class ScrappedNewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_small_news_title)
        val thumbnail: ImageView = view.findViewById(R.id.iv_small_news_thumbnail)
        // 스크랩 버튼은 스크랩 화면에서 필요 없을 수 있으므로 제거
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrappedNewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news_small, parent, false)
        return ScrappedNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScrappedNewsViewHolder, position: Int) {
        val item = newsList[position]

        // 제목 표시
        holder.title.text = item.title

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
    }

    override fun getItemCount() = newsList.size

    fun updateData(newNewsList: List<ScrappedNewsItem>) {
        newsList = newNewsList
        notifyDataSetChanged()
    }
}