package mivs.liturgicalcalendar.domain.model

import java.util.Date

data class NewsItem(
    val title: String = "",
    val content: String = "",
    val date: Date? = null,
    val actionLink: String? = null,
    val imageUrl: String? = null,
    val isVisible: Boolean = true
)