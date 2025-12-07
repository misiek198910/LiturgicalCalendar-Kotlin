package mivs.liturgicalcalendar.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "fixed_feasts", primaryKeys = ["month", "day"])
data class FixedFeastEntity(
    val month: Int,
    val day: Int,
    val feastName: String,
    val color: String,
    val rank: Int,
    val gospelSigla: String,
    val psalmResponse: String,

    @ColumnInfo(name = "psalmSigla")
    val psalmSigla: String? = "Z dnia"
)