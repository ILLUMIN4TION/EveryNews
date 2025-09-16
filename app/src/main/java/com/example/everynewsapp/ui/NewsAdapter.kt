package com.example.everynewsapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.everynewsapp.R
import com.example.everynewsapp.news.model.NewsItem
import android.content.Intent
import android.widget.Toast
import com.example.everynewsapp.NewsDetailActivity
import com.example.everynewsapp.news.viewModel.NewsDetailViewModel
import com.example.everynewsapp.news.viewModel.ViewModels

class NewsAdapter(private var newsList: List<NewsItem> = mutableListOf(), private val viewModel: NewsDetailViewModel) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_default_news_title)
        val thumbnail: ImageView = view.findViewById(R.id.iv_defaul_news_thumbnail)
        val description: TextView = view.findViewById(R.id.tv_default_news_description)

        val scrapButton: ImageView = view.findViewById(R.id.imv_news_scrap)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = newsList[position]

        //뉴스 타이틀과 내용에서 HTML 태그 제거
        deleteHTMLTag(holder, item)

        // 2. 썸네일 이미지 로드
        val imageUrl = item.imageUrl
        if (imageUrl != null) {
            holder.thumbnail.load(imageUrl) {
                crossfade(true)
            }
        } else {
            holder.thumbnail.load(R.drawable.ic_search)
        }

        // --- 여기부터 추가된 코드입니다 ---
        // 3. 각 아이템 뷰에 클릭 이벤트 리스너 설정
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            // NewsDetailActivity로 이동하기 위한 "티켓"(Intent) 생성
            val intent = Intent(context, NewsDetailActivity::class.java).apply {
                // "티켓"에 클릭된 뉴스의 모든 데이터(item)를 첨부
                putExtra("NEWS_ITEM", item)
            }
            // "티켓"을 사용하여 새 액티비티 시작
            context.startActivity(intent)
        }
        // --- 여기까지 추가된 코드입니다 ---
        holder.scrapButton.setOnClickListener {
            viewModel.toggleScrap(item)
            // 토스트 메시지나 아이콘 변경 등 UI 피드백을 추가할 수 있습니다.
            Toast.makeText(holder.itemView.context, "스크랩 상태 변경", Toast.LENGTH_SHORT).show()
        }


    }

    override fun getItemCount() = newsList.size

    fun updateData(newNewsList: List<NewsItem>) {

        newsList = newNewsList
        notifyDataSetChanged()
    }
}

private fun deleteHTMLTag(
    holder: NewsAdapter.NewsViewHolder,
    item: NewsItem
) {
    // 1. 뉴스 제목 및 설명 표시
    holder.title.text = item.title.replace("<b>", "")
        .replace("</b>", "")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
    holder.description.text = item.description.replace("<b>", "")
        .replace("</b>", "")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}