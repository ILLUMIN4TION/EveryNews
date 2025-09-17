package com.example.everynewsapp.ui

// com.example.everynewsapp.ui 패키지에 추가

import android.content.Intent
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
import kotlin.math.log

class ScrappedNewsAdapter(private var newsList: List<ScrappedNewsItem> = emptyList()) :
    RecyclerView.Adapter<ScrappedNewsAdapter.ScrappedNewsViewHolder>() {

    class ScrappedNewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_default_news_title)

        val desc: TextView = view.findViewById(R.id.tv_default_news_description)
        val thumbnail: ImageView = view.findViewById(R.id.iv_defaul_news_thumbnail)

        val scrapButton: ImageView = view.findViewById(R.id.imv_news_scrap)
        // 스크랩 버튼은 스크랩 화면에서 필요 없을 수 있으므로 제거
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrappedNewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return ScrappedNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScrappedNewsViewHolder, position: Int) {
        val item = newsList[position]

        // 제목/내용 표시
        holder.title.text = item.title
        holder.desc.text = item.description


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
            // NewsDetailActivity로 이동하기 위한 "티켓"(Intent) 생성
            val intent = Intent(context, NewsDetailActivity::class.java).apply {
                // "티켓"에 클릭된 뉴스의 모든 데이터(item)를 첨부
//                putExtra("NEWS_ITEM", item)
                putExtra("NEWS_ITEM", item)
            }
            Log.e("NewsDetailActivity", "NEWS_ITEM: $item")
            // "티켓"을 사용하여 새 액티비티 시작
            context.startActivity(intent)
        }
        //스크랩 설정
        holder.scrapButton.setOnClickListener {
            // 토스트 메시지나 아이콘 변경 등 UI 피드백을 추가할 수 있습니다.
            Toast.makeText(holder.itemView.context, "스크랩 상태 변경", Toast.LENGTH_SHORT).show()
        }

    }

    override fun getItemCount() = newsList.size

    fun updateData(newNewsList: List<ScrappedNewsItem>) {
        newsList = newNewsList
        notifyDataSetChanged()
    }
}