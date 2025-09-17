package com.example.everynewsapp.ui

import android.content.Intent
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
// import com.example.everynewsapp.news.viewModel.ViewModels // ViewModels 클래스는 직접 사용하지 않는 것으로 보임

class NewsAdapter(
    // 생성자에서 초기 데이터를 받을 수 있지만, 어댑터 내부에서 관리하는 것이 더 유연합니다.
    // private var initialNewsList: List<NewsItem> = emptyList(), // 필요하다면 초기 데이터 전달용
    private val viewModel: NewsDetailViewModel
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    // 어댑터 내부에서 데이터 목록을 관리하기 위한 MutableList
    private val newsItemsInternal = mutableListOf<NewsItem>()

    // 초기 데이터 설정 (만약 생성자에서 받는다면)
    // init {
    //     newsItemsInternal.addAll(initialNewsList)
    // }

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // ViewHolder 내부에서는 ViewBinding 사용을 권장합니다.
        // 예: private val binding: ItemNewsBinding (생성자에서 바인딩 객체 주입)
        val title: TextView = view.findViewById(R.id.tv_default_news_title)
        val thumbnail: ImageView = view.findViewById(R.id.iv_defaul_news_thumbnail)
        val description: TextView = view.findViewById(R.id.tv_default_news_description)
        // item_news.xml에 스크랩 버튼 ID가 imv_news_scrap_button_on_thumbnail 또는 다른 ID로 정의되어 있어야 합니다.
        // 현재 코드에서는 imv_news_scrap 으로 되어 있으나, 이전 XML 수정 요청에서는 다른 ID를 사용했습니다.
        // item_news.xml의 스크랩 버튼 ID와 일치시켜야 합니다.
        val scrapButton: ImageView = view.findViewById(R.id.imv_news_scrap) // 또는 R.id.imv_news_scrap 등 XML에 정의된 ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false) // R.layout.item_news 사용 확인
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        if (position < newsItemsInternal.size) { // 범위 확인
            val item = newsItemsInternal[position]

            // 뉴스 타이틀과 내용에서 HTML 태그 제거
            deleteHTMLTag(holder, item)

            // 썸네일 이미지 로드
            val imageUrl = item.imageUrl
            if (!imageUrl.isNullOrEmpty()) { // null 또는 빈 문자열 체크
                holder.thumbnail.load(imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지 Todo 아이콘 변경해야합니다.
                    error(android.R.drawable.stat_notify_error)       // 오류 시 이미지 (ic_image_broken.xml 드로어블 필요) Todo 아이콘 변경해야합니다.
                }
            } else {
                holder.thumbnail.setImageResource(R.drawable.ic_search) // 기본 이미지 (또는 ic_search 등)
            }

            // 각 아이템 뷰에 클릭 이벤트 리스너 설정
            holder.itemView.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, NewsDetailActivity::class.java).apply {
                    putExtra("NEWS_ITEM", item) // NewsItem은 Parcelable이어야 함
                }
                context.startActivity(intent)
                // "최근 본 뉴스" 기능이 배제되었으므로 viewModel.onNewsViewed(item) 호출은 제거하거나 주석 처리
            }

            // 스크랩 버튼 클릭 리스너
            holder.scrapButton.setOnClickListener {
                viewModel.toggleScrap(item) // NewsItem 객체 전달
                // 스크랩 상태에 따라 아이콘 변경 등의 UI 피드백은
                // ViewModel의 LiveData/StateFlow를 Activity/Fragment에서 관찰하여 어댑터에 알리는 것이 좋음
                // 예: viewModel.isScrapped.observe(...) { isScrapped -> ... }
                Toast.makeText(holder.itemView.context, "스크랩 상태가 변경되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = newsItemsInternal.size

    /**
     * 기존 데이터를 모두 지우고 새로운 데이터 목록으로 교체합니다.
     * @param newNewsList 새로운 뉴스 아이템 목록
     */
    fun updateData(newNewsList: List<NewsItem>) {
        newsItemsInternal.clear()
        newsItemsInternal.addAll(newNewsList)
        notifyDataSetChanged()
    }

    /**
     * 기존 데이터 목록의 끝에 새로운 데이터 목록을 추가합니다. (무한 스크롤용)
     * @param additionalItems 추가할 뉴스 아이템 목록
     */
    fun addData(additionalItems: List<NewsItem>) {
        val startPosition = newsItemsInternal.size
        newsItemsInternal.addAll(additionalItems)
        notifyItemRangeInserted(startPosition, additionalItems.size)
    }

    /**
     * 어댑터가 가지고 있는 모든 데이터를 지웁니다.
     */
    fun clearData() {
        newsItemsInternal.clear()
        notifyDataSetChanged()
    }
}

/**
 * ViewHolder와 NewsItem 객체를 받아 HTML 태그를 제거하고 텍스트를 설정하는 유틸리티 함수입니다.
 * NewsAdapter 클래스 바깥으로 빼거나, ViewHolder의 확장 함수 등으로 만들 수도 있습니다.
 */
private fun deleteHTMLTag(
    holder: NewsAdapter.NewsViewHolder, // NewsViewHolder 직접 참조 대신, TextView들을 직접 받는 것이 더 좋을 수 있음
    item: NewsItem
) {
    // 텍스트가 null일 가능성에 대비 (NewsItem 모델 정의에 따라)
    holder.title.text = item.title?.replace("<b>", "")
        ?.replace("</b>", "")
        ?.replace("&quot;", "\"")
        ?.replace("&amp;", "&")
        ?.replace("&lt;", "<")
        ?.replace("&gt;", ">") ?: "" // null이면 빈 문자열

    holder.description.text = item.description?.replace("<b>", "")
        ?.replace("</b>", "")
        ?.replace("&quot;", "\"")
        ?.replace("&amp;", "&")
        ?.replace("&lt;", "<")
        ?.replace("&gt;", ">") ?: "" // null이면 빈 문자열
}

