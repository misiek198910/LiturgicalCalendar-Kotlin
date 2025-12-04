package mivs.liturgicalcalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "psalm_texts")
data class PsalmEntity(
    @PrimaryKey val sigla: String, // np. "Ps 23, 1-4"
    val content: String            // Treść psalmu
)