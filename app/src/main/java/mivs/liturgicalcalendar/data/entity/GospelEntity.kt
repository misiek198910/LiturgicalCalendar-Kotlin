package mivs.liturgicalcalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gospel_texts")
data class GospelEntity(
    @PrimaryKey val sigla: String, 
    val content: String            
)