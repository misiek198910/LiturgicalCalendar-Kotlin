// UserStatusDao.kt
package mivs.liturgicalcalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mivs.liturgicalcalendar.data.entity.UserStatusEntity

@Dao
interface UserStatusDao {

    // Używamy REPLACE, żeby zawsze nadpisywać jedyny wiersz
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: UserStatusEntity)

    @Query("SELECT * FROM user_status WHERE id = 1")
    suspend fun getStatus(): UserStatusEntity?
}