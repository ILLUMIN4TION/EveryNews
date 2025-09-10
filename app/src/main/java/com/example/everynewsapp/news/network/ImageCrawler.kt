package com.example.everynewsapp.news.network

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ImageCrawler {

    fun extractImageUrl(htmlContent: String, baseUrl: String): String? {
        val doc = Jsoup.parse(htmlContent, baseUrl)

        // 우선순위 1: Open Graph (og:image) - 가장 표준적이고 안정적
        var imageUrl = doc.select("meta[property=og:image]").firstOrNull()?.attr("abs:content")
        if (isValidImageUrl(imageUrl)) {
            return imageUrl
        }

        // 우선순위 2: 주요 뉴스 콘텐츠 영역 내의 이미지 (선택자 예시, 사이트 분석 필요)
        // 예시: <div class="article-body"> 또는 <article> 태그 내부의 이미지
        // 이 부분은 각 사이트의 공통적인 콘텐츠 영역 선택자를 찾아야 더 효과적입니다.
        // val mainContentElement = doc.select(".article_view, .article_body, #article_txt, #articleText, article").firstOrNull()
        // if (mainContentElement != null) {
        //     imageUrl = findBestImageInElement(mainContentElement)
        //     if (isValidImageUrl(imageUrl)) {
        //         return imageUrl
        //     }
        // }


        // 우선순위 3: 특정 ID 패턴 (예시, 계속 추가/관리 필요)
        val knownIdSelectors = listOf("img#im_n1", "img#img_pop_view", "img#mainPhoto") // 예시 ID 목록
        for (selector in knownIdSelectors) {
            imageUrl = doc.select(selector).firstOrNull()?.attr("abs:src")
            if (isValidImageUrl(imageUrl)) {
                return imageUrl
            }
        }

        // 우선순위 4: 이미지 URL에 썸네일 관련 키워드가 포함된 경우
        // 또는 특정 경로 패턴을 가진 이미지 (더 정교한 정규식 또는 로직 필요)
        val keywordHintImages = doc.select("img[src*=/thumb/], img[src*=thumbnail], img[src*=/photo/], img[src*=/cache/], img[src*=/image/]")
        for (img in keywordHintImages) {
            imageUrl = img.attr("abs:src")
            if (isValidImageUrl(imageUrl) && isPlausibleSize(img)) { // 추가적인 크기 검증
                return imageUrl
            }
        }

        // 우선순위 5: alt 속성이 있고, 이미지 확장자를 가진 이미지 중 첫번째
        // (광고나 아이콘이 아닌 실제 콘텐츠 이미지일 가능성을 높임)
        val imagesWithAlt = doc.select("img[alt][alt!=''][src~=(?i)\\.(png|jpe?g|gif|webp)]")
        for (img in imagesWithAlt) {
            imageUrl = img.attr("abs:src")
            if (isValidImageUrl(imageUrl) && isPlausibleSize(img)) {
                return imageUrl
            }
        }


        // 우선순위 6: 확장자를 가진 모든 이미지 중 첫 번째 (너무 작지 않은 것)
        val allImagesWithExt = doc.select("img[src~=(?i)\\.(png|jpe?g|gif|webp)]")
        for (img in allImagesWithExt) {
            imageUrl = img.attr("abs:src")
            if (isValidImageUrl(imageUrl) && isPlausibleSize(img)) {
                return imageUrl
            }
        }


        // 최후의 수단: 그냥 첫번째 이미지 (가장 위험)
        // imageUrl = doc.select("img").firstOrNull()?.attr("abs:src")
        // if (isValidImageUrl(imageUrl)) {
        //     return imageUrl
        // }

        return null // 모든 방법으로 이미지를 찾지 못한 경우
    }

    /**
     * URL이 유효한 HTTP(S) URL인지 간단히 확인
     */
    private fun isValidImageUrl(url: String?): Boolean {
        return url != null && url.startsWith("http") &&
                (url.endsWith(".jpg", true) || url.endsWith(".jpeg", true) ||
                        url.endsWith(".png", true) || url.endsWith(".gif", true) || url.endsWith(".webp", true) ||
                        url.contains(".jpg?", true) || url.contains(".jpeg?", true) || // 파라미터가 붙는 경우
                        url.contains(".png?", true) || url.contains(".gif?", true) || url.contains(".webp?", true) ||
                        !url.substringAfterLast("/", "").contains(".") // URL 경로 마지막에 확장자가 없는 경우도 일단 허용 (dims/optimize/ 같은 경우)
                        )
    }

    /**
     * 이미지 태그의 width/height 속성이나 URL 패턴으로 너무 작은 이미지가 아닌지 추정
     */
    private fun isPlausibleSize(imgElement: Element): Boolean {
        // width, height 속성 직접 확인
        val width = imgElement.attr("width").toIntOrNull() ?: 0
        val height = imgElement.attr("height").toIntOrNull() ?: 0

        if (width > 0 && height > 0) { // width, height 속성이 명시적으로 있는 경우
            return width >= 100 && height >= 100 // 예: 최소 100x100 픽셀
        }

        // 스타일 속성에서 크기 정보 파싱 (간단한 예시, 복잡한 CSS는 파싱 어려움)
        val style = imgElement.attr("style")
        if (style.contains("width") && style.contains("height")) {
            // 정규식 등으로 px 값 추출 시도 가능 (여기서는 단순화)
            // 예: "width:150px" -> 150
        }

        // URL에 크기 정보가 포함된 경우 (예: /150x100/)
        // 또는 특정 키워드가 없는 경우 (예: /icon/, /logo/)
        // 이 부분은 더 정교한 패턴 분석이 필요

        return true // 일단 기본적인 필터링만 통과하면 true (더 개선 필요)
    }

    // (선택 사항) 특정 요소 내에서 최적의 이미지를 찾는 함수
    // private fun findBestImageInElement(element: Element): String? {
    //     // 요소 내의 이미지들 중 크기, alt 텍스트 등을 고려하여 최적의 이미지 선택
    //     // ...
    //     return null
    // }
}

