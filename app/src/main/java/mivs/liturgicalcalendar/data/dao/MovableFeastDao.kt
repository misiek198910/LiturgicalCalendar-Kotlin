package mivs.liturgicalcalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mivs.liturgicalcalendar.data.entity.MovableFeastEntity

@Dao
interface MovableFeastDao {

    /**
     * Główna metoda używana w CalendarRepository.
     * Dzięki "SELECT *" i zaktualizowanej Encji, Room automatycznie
     * pobierze też nową kolumnę 'psalmSigla'.
     */
    @Query("SELECT * FROM movable_feasts WHERE `key` = :key")
    suspend fun getFeast(key: String): MovableFeastEntity?

    // --- Metody pomocnicze (opcjonalne, jeśli ich używasz np. przy inicjalizacji bazy) ---

    @Query("SELECT * FROM movable_feasts")
    suspend fun getAllFeasts(): List<MovableFeastEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(feasts: List<MovableFeastEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feast: MovableFeastEntity)

    @Query("DELETE FROM movable_feasts")
    suspend fun clearAll()
}