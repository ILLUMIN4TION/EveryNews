package com.example.everynewsapp.news.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.everynewsapp.R
import com.example.everynewsapp.news.model.NewsItem

class NewsAdapter(
    private var newsList: List<NewsItem>,
    private val onBookmarkClick: (NewsItem) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.newsTitle)
        val thumbnail: ImageView = view.findViewById(R.id.newsThumbnail)
        val description: TextView = view.findViewById(R.id.newsDescription)
        val bookmarkButton: ImageButton = view.findViewById(R.id.bookmarkButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = newsList[position]
        holder.title.text = item.title.replace("<b>", "").replace("</b>", "")
        holder.description.text = item.description.replace("<b>", "").replace("</b>", "")
        holder.thumbnail.load("https://img.icons8.com/color/480/news.png")

        // 북마크 버튼 클릭
        holder.bookmarkButton.setOnClickListener {
            onBookmarkClick(item)
        }
        holder.itemView.setOnClickListener {
            val intent = Intent(it.context, NewsDetailActivity::class.java)
            intent.putExtra("title", item.title.replace("<b>", "").replace("</b>", ""))
            intent.putExtra("description", item.description.replace("<b>", "").replace("</b>", ""))
            intent.putExtra("pubDate", item.pubDate)
            intent.putExtra("link", item.link)
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount() = newsList.size
}
