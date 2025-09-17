package com.example.everynewsapp.ui

import android.content.Intent
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
import com.example.everynewsapp.news.model.NewsItem
import com.example.everynewsapp.news.viewModel.NewsDetailViewModel

class TrendingNewsAdapter(private var newsList: List<NewsItem> = mutableListOf(),private val viewModel: NewsDetailViewModel) : // ★ newsList를 val에서 var로 변경 ★
    RecyclerView.Adapter<TrendingNewsAdapter.TrendingNewsViewHolder>() {

    class TrendingNewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_small_news_title)
        val thumbnail: ImageView = view.findViewById(R.id.iv_small_news_thumbnail)

        val scrapButton: ImageView = view.findViewById(R.id.iv_small_news_scrap_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingNewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news_small, parent, false)
        return TrendingNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrendingNewsViewHolder, position: Int) {
        val item = newsList[position]

        // ★ HTML.fromHtml()을 사용하여 HTML 태그와 엔티티를 자동으로 변환 ★
        deleteHTMLTag(holder, item)

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
            // 토스트 메시지나 아이콘 변경 등 UI 피드백을 추가할 수 있습니다.
            Toast.makeText(holder.itemView.context, "스크랩 상태 변경", Toast.LENGTH_SHORT).show()
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
private fun deleteHTMLTag(
    holder: TrendingNewsAdapter.TrendingNewsViewHolder,
    item: NewsItem
) {
    // 1. 뉴스 제목 및 설명 표시
    holder.title.text = item.title.replace("<b>", "")
        .replace("</b>", "")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}