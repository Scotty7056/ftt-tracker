package com.example.freizeittickettracker.data

import com.example.freizeittickettracker.data.model.PersonProfile
import com.example.freizeittickettracker.data.model.VisitPersonCrossRef
import com.example.freizeittickettracker.data.dao.PersonDao

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? = date?.toString()
}

@Database(entities = [VisitEntity::class, TicketConfigEntity::class, PersonProfile::class, VisitPersonCrossRef::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun visitDao(): VisitDao
    abstract fun ticketConfigDao(): TicketConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN season TEXT NOT NULL DEFAULT '2024/25'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "freizeitticket_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
    abstract fun personDao(): PersonDao
}
