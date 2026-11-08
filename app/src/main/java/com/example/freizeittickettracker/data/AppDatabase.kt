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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS persons (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        birthYear INTEGER NOT NULL,
                        isEarlyBird INTEGER NOT NULL DEFAULT 1,
                        customTicketPrice REAL,
                        isDefaultSelected INTEGER NOT NULL DEFAULT 1,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS visit_person_cross_ref (
                        visitId INTEGER NOT NULL,
                        personId INTEGER NOT NULL,
                        PRIMARY KEY(visitId, personId),
                        FOREIGN KEY(visitId) REFERENCES visits(id) ON DELETE CASCADE,
                        FOREIGN KEY(personId) REFERENCES persons(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_visit_person_cross_ref_personId ON visit_person_cross_ref(personId)")
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
