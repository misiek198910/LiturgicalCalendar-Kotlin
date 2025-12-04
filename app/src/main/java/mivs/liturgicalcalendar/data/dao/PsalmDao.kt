package mivs.liturgicalcalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mivs.liturgicalcalendar.data.entity.PsalmEntity

@Dao
interface PsalmDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(psalm: PsalmEntity)

    @Query("SELECT content FROM psalm_texts WHERE sigla = :sigla")
    suspend fun getPsalm(sigla: String): String?
}