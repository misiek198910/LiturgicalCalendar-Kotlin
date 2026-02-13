package mivs.liturgicalcalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "psalm_texts")
data class PsalmEntity(
    @PrimaryKey val sigla: String, 
    val content: String            
)