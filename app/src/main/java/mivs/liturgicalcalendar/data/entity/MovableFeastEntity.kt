package mivs.liturgicalcalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movable_feasts")
data class MovableFeastEntity(
    @PrimaryKey val key: String, // np. "ASH_WEDNESDAY", "EASTER_SUNDAY"
    val gospelSigla: String,
    val psalmResponse: String
    // Nazwę i Kolor bierzemy z algorytmu, tu trzymamy tylko teksty
)