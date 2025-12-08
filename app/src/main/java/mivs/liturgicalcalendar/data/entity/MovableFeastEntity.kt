package mivs.liturgicalcalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movable_feasts")
data class MovableFeastEntity(
    @PrimaryKey val key: String,
    val gospelSigla: String,
    val psalmResponse: String,
    val psalmSigla: String? = null // Nowe pole (może być null dla wstecznej kompatybilności)
)