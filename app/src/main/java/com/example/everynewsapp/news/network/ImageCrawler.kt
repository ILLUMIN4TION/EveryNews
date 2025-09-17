package com.example.everynewsapp.news.network

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ImageCrawler {

    fun extractImageUrl(htmlContent: String, baseUrl: String): String? {
        val doc = Jsoup.parse(htmlContent, baseUrl)

        // 1. Open Graph 이미지
        var imageUrl = doc.select("meta[property=og:image]").firstOrNull()?.attr("content")
        if (isValidImageUrl(imageUrl)) {
            return resolveUrl(baseUrl, imageUrl)
        }

        // 2. 본문 영역 내 이미지 (alt 없어도 허용)
        val contentImages = doc.select(
            "#newsct_article img[src], .article_body img[src], #article_txt img[src], #dic_area img[src], article img[src]"
        )
        for (img in contentImages) {
            val src = getImageSrc(img)
            if (isValidImageUrl(src) && isPlausibleSize(img)) {
                return src
            }
        }

        // 3. 특정 ID 패턴
        val knownIdSelectors = listOf("img#im_n1", "img#img_pop_view", "img#mainPhoto")
        for (selector in knownIdSelectors) {
            val src = getImageSrc(doc.select(selector).firstOrNull() ?: continue)
            if (isValidImageUrl(src)) {
                return src
            }
        }

        // 4. 썸네일/사진 힌트 포함 이미지
        val keywordHintImages = doc.select(
            "img[src*=/thumb/], img[src*=thumbnail], img[src*=/photo/], img[src*=/cache/], img[src*=/image/]"
        )
        for (img in keywordHintImages) {
            val src = getImageSrc(img)
            if (isValidImageUrl(src) && isPlausibleSize(img)) {
                return src
            }
        }

        // 5. alt 속성 있는 이미지 (광고/배너 제외)
        val imagesWithAlt = doc.select("img[alt][src~=(?i)\\.(png|jpe?g|gif|webp)]")
        for (img in imagesWithAlt) {
            val alt = img.attr("alt")
            if (alt.contains("광고") || alt.contains("배너") || alt.contains("아이콘")) continue
            val src = getImageSrc(img)
            if (isValidImageUrl(src) && isPlausibleSize(img)) {
                return src
            }
        }

        // 6. 확장자 있는 모든 이미지 중 첫 번째
        val allImages = doc.select("img[src~=(?i)\\.(png|jpe?g|gif|webp)]")
        for (img in allImages) {
            val src = getImageSrc(img)
            if (isValidImageUrl(src) && isPlausibleSize(img)) {
                return src
            }
        }

        return null
    }

    /** baseUrl 기준 절대경로 변환 */
    private fun resolveUrl(baseUrl: String, url: String?): String? {
        return if (url != null && !url.startsWith("http")) {
            Jsoup.parse("<a href='$url'/>", baseUrl).select("a").firstOrNull()?.attr("abs:href")
        } else url
    }

    /** 이미지 src 가져오기 (absUrl 실패 시 fallback) */
    private fun getImageSrc(img: Element): String? {
        val abs = img.absUrl("src")
        if (abs.isNotBlank()) return abs

        val raw = img.attr("src")
        if (raw.isNotBlank()) return raw

        return null
    }

    /** 본문 내 최적의 이미지 선택 (가장 큰 크기) */
    private fun findBestImageInElement(element: Element, baseUrl: String): String? {
        val candidates = element.select("img[src]")
        return candidates
            .map { getImageSrc(it) to it }
            .filter { (src, el) -> isValidImageUrl(src) && isPlausibleSize(el) }
            .maxByOrNull { (_, el) ->
                (el.attr("width").toIntOrNull() ?: 0) *
                        (el.attr("height").toIntOrNull() ?: 0)
            }
            ?.first
    }

    /** URL이 유효한지 체크 */
    private fun isValidImageUrl(url: String?): Boolean {
        return url != null && url.startsWith("http") &&
                (url.endsWith(".jpg", true) || url.endsWith(".jpeg", true) ||
                        url.endsWith(".png", true) || url.endsWith(".gif", true) || url.endsWith(".webp", true) ||
                        url.contains(".jpg?", true) || url.contains(".jpeg?", true) ||
                        url.contains(".png?", true) || url.contains(".gif?", true) || url.contains(".webp?", true) ||
                        !url.substringAfterLast("/", "").contains("."))
    }

    /** plausible size 판단 (너무 작은 아이콘/광고 제외) */
    private fun isPlausibleSize(imgElement: Element): Boolean {
        val width = imgElement.attr("width").toIntOrNull() ?: 0
        val height = imgElement.attr("height").toIntOrNull() ?: 0

        if (width > 0 && height > 0) {
            return width >= 100 && height >= 100
        }

        val src = imgElement.attr("src")
        if (src.contains("icon") || src.contains("logo") || src.contains("banner")) {
            return false
        }

        return true
    }
}
