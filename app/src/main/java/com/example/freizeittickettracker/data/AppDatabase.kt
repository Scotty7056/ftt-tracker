package com.example.freizeittickettracker.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY date DESC, id DESC")
    fun getAllVisits(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits ORDER BY date DESC, id DESC LIMIT 5")
    fun getRecentVisits(): Flow<List<VisitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity): Long

    @Delete
    suspend fun deleteVisit(visit: VisitEntity)
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM ticket_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<TicketConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: TicketConfigEntity)
}

@Database(
    entities = [VisitEntity::class, TicketConfigEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun visitDao(): VisitDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "freizeitticket_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}