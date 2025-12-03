package mivs.liturgicalcalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mivs.liturgicalcalendar.data.entity.GospelEntity

@Dao
interface GospelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gospel: GospelEntity)

    @Query("SELECT content FROM gospel_texts WHERE sigla = :sigla")
    suspend fun getGospel(sigla: String): String?

    @Query("SELECT COUNT(*) FROM gospel_texts")
    suspend fun getCount(): Int
}