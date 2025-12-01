package mivs.liturgicalcalendar.data.entity

import androidx.room.Entity

// Kluczem głównym jest kombinacja miesiąca i dnia (np. 11-01)
@Entity(tableName = "fixed_feasts", primaryKeys = ["month", "day"])
data class FixedFeastEntity(
    val month: Int,      // np. 11
    val day: Int,        // np. 1
    val feastName: String, // "Wszystkich Świętych"
    val color: String,     // "w", "r", "v", "g"
    val rank: Int          // 3=Uroczystość, 2=Święto, 1=Wspomnienie
)