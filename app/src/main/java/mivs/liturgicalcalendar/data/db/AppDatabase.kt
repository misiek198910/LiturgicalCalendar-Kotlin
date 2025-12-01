package mivs.liturgicalcalendar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mivs.liturgicalcalendar.data.dao.DayDao
import mivs.liturgicalcalendar.data.dao.FixedFeastDao
import mivs.liturgicalcalendar.data.dao.MovableFeastDao
import mivs.liturgicalcalendar.data.entity.DayEntity
import mivs.liturgicalcalendar.data.entity.FixedFeastEntity // <--- TEGO BRAKOWAŁO (Import)

// <--- TEGO BRAKOWAŁO W LINIJCE NIŻEJ (Dodanie do entities)
@Database(entities = [DayEntity::class, FixedFeastEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun fixedFeastDao(): FixedFeastDao
    abstract fun movableFeastDao(): MovableFeastDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "liturgical_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}