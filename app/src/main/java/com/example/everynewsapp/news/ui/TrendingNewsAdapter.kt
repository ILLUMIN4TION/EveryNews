package com.example.everynewsapp.news.ui

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

        // 1. 뉴스 제목 표시 (HTML 태그 제거 및 HTML 엔티티 변환)
        var newsTitle = item.title
        newsTitle = newsTitle.replace("<b>", "")
        newsTitle = newsTitle.replace("</b>", "")
        newsTitle = newsTitle.replace("&quot;", "\"") // &quot;를 "로 변경
        newsTitle = newsTitle.replace("&amp;", "&")   // &amp;를 &로 변경 (추가적인 예시)
        newsTitle = newsTitle.replace("&lt;", "<")    // &lt;를 <로 변경 (추가적인 예시)
        newsTitle = newsTitle.replace("&gt;", ">")    // &gt;를 >로 변경 (추가적인 예시)
        // 다른 HTML 엔티티들도 필요에 따라 추가


        holder.title.text = newsTitle

        // 2. 썸네일 이미지 로드
        val imageUrl = item.imageUrl
        if (imageUrl != null) {
            // 크롤링된 이미지가 있는 경우, 해당 URL을 로드
            holder.thumbnail.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_search) // 로딩 중 표시할 이미지 (선택 사항)
                error(R.drawable.ic_search)       // 에러 시 표시할 이미지 (선택 사항)
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
        // 기존 newsList를 직접 수정하는 것보다 새로운 리스트로 교체하고
        // DiffUtil을 사용하는 것이 더 효율적이고 애니메이션 효과도 자연스럽습니다.
        // 여기서는 질문의 핵심에 집중하기 위해 기존 방식을 유지합니다.
        (newsList as ArrayList).clear()
        (newsList as ArrayList).addAll(newNewsList)
        notifyDataSetChanged()
    }
}
