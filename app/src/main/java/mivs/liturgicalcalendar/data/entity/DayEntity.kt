package mivs.liturgicalcalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "liturgical_days")
data class DayEntity(
    @PrimaryKey val date: LocalDate, // Unikalny klucz: Data
    val feastName: String,
    val colorCode: String,
    val gospelSigla: String,
    val psalmResponse: String
)