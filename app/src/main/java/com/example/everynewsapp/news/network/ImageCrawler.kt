package com.example.everynewsapp.news.network

import org.jsoup.Jsoup

class ImageCrawler {

    fun extractImageUrl(htmlContent: String, baseUrl: String): String? {
        val doc = Jsoup.parse(htmlContent, baseUrl)

        // 1. Open Graph 메타 태그에서 og:image 찾기 (가장 일반적인 방법)
        var imageUrl = doc.select("meta[property=og:image]").firstOrNull()?.attr("abs:content")
        if (imageUrl != null && imageUrl.startsWith("http")) {
            return imageUrl
        }

        // 2. 특정 ID를 가진 이미지 태그 찾기 (예: "im_n1")
        //    사이트마다 ID가 다를 수 있으므로, 여러 주요 사이트의 패턴을 추가하거나 더 일반적인 방법을 고려해야 함
        imageUrl = doc.select("img#im_n1").firstOrNull()?.attr("abs:src")
        if (imageUrl != null && imageUrl.startsWith("http")) {
            return imageUrl
        }

        // 3. (선택적) 더 구체적인 CSS 선택자 사용 (사이트 구조 분석 필요)
        //    예: val specificImageTag = doc.select("div.article_photo img").firstOrNull()
        //    imageUrl = specificImageTag?.attr("abs:src")
        //    if (imageUrl != null && imageUrl.startsWith("http")) {
        //        return imageUrl
        //    }


        // 4. 위 방법들이 모두 실패하면, 확장자를 가진 첫 번째 유의미한 <img> 태그 찾기
        //    (너무 작은 이미지를 피하기 위한 추가적인 필터링 로직을 고려할 수 있음)
        val imageTag = doc.select("img[src~=(?i)\\.(png|jpe?g|gif|webp)]").firstOrNull { img ->
            // 여기에 이미지 크기나 다른 조건으로 필터링하는 로직 추가 가능
            // 예: (img.attr("width").toIntOrNull() ?: 0) > 50 && (img.attr("height").toIntOrNull() ?: 0) > 50
            true // 일단은 확장자만으로 필터링
        }
        imageUrl = imageTag?.attr("abs:src")
        if (imageUrl != null && imageUrl.startsWith("http")) {
            return imageUrl
        }

        // 5. 정말 마지막으로, 아무 img 태그 중 첫번째 (위험 부담 있음)
        imageUrl = doc.select("img").firstOrNull()?.attr("abs:src")
        if (imageUrl != null && imageUrl.startsWith("http")) {
            return imageUrl
        }

        return null // 모든 방법으로 이미지를 찾지 못한 경우
    }
}
