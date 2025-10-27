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
            .inflate(R.layout.item_news, parent, false) // item_news.xml 사용 (기존과 동일)
        return ScrappedNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScrappedNewsViewHolder, position: Int) {
        val item = newsList[position]

        holder.title.text = item.title.stripHtml()
        holder.desc.text = item.description?.stripHtml()

        // 이미지 로딩 로직 (기존과 동일)
        if (!item.imageUrl.isNullOrEmpty()) {
            holder.thumbnail.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_search) // 로딩 중 이미지
                error(R.drawable.ic_search)       // 오류 시 이미지
            }
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_search) // URL 없으면 기본 이미지
        }

        // 아이템 클릭 시 상세 화면 이동 (기존과 동일)
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, NewsDetailActivity::class.java).apply {
                // ScrappedNewsItem을 NewsItem으로 변환하거나 Parcelable 처리 필요할 수 있음
                putExtra("NEWS_ITEM", item.toNewsItem()) // 예시: toNewsItem() 확장 함수 사용
            }
            Log.d("ScrappedNewsAdapter", "Starting NewsDetailActivity with: ${item.title}")
            context.startActivity(intent)
        }

        // 스크랩 버튼 클릭 시 스크랩 해제 (기존과 동일)
        holder.scrapButton.setOnClickListener {
            newsViewModel.removeScrap(item)
            Toast.makeText(holder.itemView.context, "스크랩 해제됨", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = newsList.size

    // 데이터 업데이트 함수 (기존과 동일)
    fun updateData(newNewsList: List<ScrappedNewsItem>) {
        newsList.clear()
        newsList.addAll(newNewsList)
        notifyDataSetChanged() // DiffUtil 사용 권장
    }

    // HTML 태그 제거 함수 (기존과 동일)
    private fun String?.stripHtml(): String {
        if (this == null) return ""
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(this).toString()
        }
    }

    // ★★★ ViewHolder 수정 ★★★
    class ScrappedNewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_default_news_title)
        val desc: TextView = view.findViewById(R.id.tv_default_news_description)
        // ★★★ 이미지뷰 ID를 item_news.xml과 일치시킴 ★★★
        val thumbnail: ImageView = view.findViewById(R.id.iv_default_news_thumbnail) // 수정된 ID
        val scrapButton: ImageView = view.findViewById(R.id.imv_news_scrap)
    }
}
