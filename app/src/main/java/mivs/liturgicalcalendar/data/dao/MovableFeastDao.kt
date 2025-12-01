package mivs.liturgicalcalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mivs.liturgicalcalendar.data.entity.MovableFeastEntity

@Dao
interface MovableFeastDao {
    @Query("SELECT * FROM movable_feasts WHERE `key` = :key")
    suspend fun getFeast(key: String): MovableFeastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(feasts: List<MovableFeastEntity>)

    @Query("SELECT COUNT(*) FROM movable_feasts")
    suspend fun getCount(): Int
}