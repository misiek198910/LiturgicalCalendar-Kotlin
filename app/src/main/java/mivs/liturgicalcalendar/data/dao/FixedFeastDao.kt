package mivs.liturgicalcalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mivs.liturgicalcalendar.data.entity.FixedFeastEntity

@Dao
interface FixedFeastDao {
    // Pobierz święto dla konkretnego dnia i miesiąca
    @Query("SELECT * FROM fixed_feasts WHERE month = :month AND day = :day")
    suspend fun getFeast(month: Int, day: Int): FixedFeastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(feasts: List<FixedFeastEntity>)

    @Query("SELECT COUNT(*) FROM fixed_feasts")
    suspend fun getCount(): Int
}