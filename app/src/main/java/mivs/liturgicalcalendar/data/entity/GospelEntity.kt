package mivs.liturgicalcalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gospel_texts")
data class GospelEntity(
    @PrimaryKey val sigla: String, // Klucz, np. "MT5,1–12A"
    val content: String            // Treść Ewangelii
)