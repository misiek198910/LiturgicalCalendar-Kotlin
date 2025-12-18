package mivs.liturgicalcalendar.domain.model

import java.util.Date

data class NewsItem(
    // Wartości domyślne (= "", = null) są KONIECZNE dla Firebase!
    val title: String = "",
    val content: String = "",

    // Upewnij się, że to jest java.util.Date
    val date: Date? = null,

    val actionLink: String? = null,
    val imageUrl: String? = null,

    // Adnotacja @field:PropertyName pomaga, jeśli nazwa pola to "isVisible" (problem z getterami w Javie/Kotlinie)
    @field:JvmField // lub @get:PropertyName("isVisible")
    val isVisible: Boolean = true
)