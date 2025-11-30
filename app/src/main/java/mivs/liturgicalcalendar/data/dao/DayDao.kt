package mivs.liturgicalcalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mivs.liturgicalcalendar.data.entity.DayEntity
import java.time.LocalDate

@Dao
interface DayDao {
    // Pobierz jeden dzień
    @Query("SELECT * FROM liturgical_days WHERE date = :date")
    suspend fun getDay(date: LocalDate): DayEntity?

    // Wstaw listę dni (import startowy)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<DayEntity>)

    // Sprawdź ile jest dni w bazie
    @Query("SELECT COUNT(*) FROM liturgical_days")
    suspend fun getCount(): Int
}