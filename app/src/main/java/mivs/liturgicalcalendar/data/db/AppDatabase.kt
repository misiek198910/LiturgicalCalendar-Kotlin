package mivs.liturgicalcalendar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mivs.liturgicalcalendar.data.dao.FixedFeastDao
import mivs.liturgicalcalendar.data.dao.GospelDao
import mivs.liturgicalcalendar.data.dao.MovableFeastDao
import mivs.liturgicalcalendar.data.dao.PsalmDao
import mivs.liturgicalcalendar.data.dao.UserStatusDao
import mivs.liturgicalcalendar.data.entity.FixedFeastEntity
import mivs.liturgicalcalendar.data.entity.GospelEntity
import mivs.liturgicalcalendar.data.entity.MovableFeastEntity
import mivs.liturgicalcalendar.data.entity.PsalmEntity
import mivs.liturgicalcalendar.data.entity.UserStatusEntity

@Database(
    entities = [FixedFeastEntity::class, GospelEntity::class, MovableFeastEntity::class, PsalmEntity::class, UserStatusEntity::class],
    version = 40,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fixedFeastDao(): FixedFeastDao
    abstract fun movableFeastDao(): MovableFeastDao
    abstract fun gospelDao(): GospelDao
    abstract fun psalmDao(): PsalmDao
    abstract fun userStatusDao(): UserStatusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "liturgical_v2.db"
                )
                    .createFromAsset("liturgical_v2.db")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}