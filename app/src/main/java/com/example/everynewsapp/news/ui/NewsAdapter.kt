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

class NewsAdapter(private var newsList: List<NewsItem>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.newsTitle)
        val thumbnail: ImageView = view.findViewById(R.id.newsThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = newsList[position]
        holder.title.text = item.title.replace("<b>", "").replace("</b>", "")
        holder.thumbnail.load("https://img.icons8.com/color/480/news.png") // 썸네일 기본 아이콘
    }

    override fun getItemCount() = newsList.size
}
