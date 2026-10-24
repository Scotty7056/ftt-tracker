package com.example.freizeittickettracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.Month
import java.time.ZoneId

enum class Category(val displayName: String) {
    SKI("Skigebiete & Bergbahnen"),
    SWIMMING("Hallen- & Freibäder"),
    LAKE("Badeseen"),
    ICE_SKATING("Eislaufplätze"),
    CULTURE("Museen & Kultur")
}

enum class TicketType(val label: String, val presalePrice: Double, val regularPrice: Double) {
    ADULT("Erwachsene (1962–2002)", 789.0, 908.0),
    U23("U23 (2003–2007)", 710.0, 817.0),
    YOUTH("Jugend (2008–2010)", 631.0, 725.0),
    CHILD("Kinder (ab 2011)", 410.0, 472.0),
    SENIOR("Senioren (ab 1961)", 750.0, 862.0),
    DISABLED("Menschen mit Behinderung", 592.0, 681.0)
}

data class Venue(
    val id: String,
    val name: String,
    val category: Category,
    val adultPrice: Double,
    val u23Price: Double,
    val youthPrice: Double,
    val childPrice: Double,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Double = 1500.0,
    val webcamImageUrl: String? = null,
    val webcamLiveUrl: String? = null
) {
    fun getPriceFor(ticketType: TicketType): Double = when (ticketType) {
        TicketType.ADULT, TicketType.SENIOR, TicketType.DISABLED -> adultPrice
        TicketType.U23 -> u23Price
        TicketType.YOUTH -> youthPrice
        TicketType.CHILD -> childPrice
    }
}

enum class Season(val displayName: String) {
    WINTER("Winter (Okt – Apr)"),
    SUMMER("Sommer (Mai – Okt)");

    companion object {
        fun fromTimestamp(epochMillis: Long): Season {
            val localDate = Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            return when (localDate.month) {
                Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER, Month.OCTOBER -> SUMMER
                else -> WINTER
            }
        }
    }
}

data class SeasonStats(
    val season: Season,
    val totalAmount: Double,
    val visitCount: Int,
    val categoryBreakdown: Map<String, Double>
) {
    fun percentageOf(totalTicketCost: Double): Float =
        if (totalTicketCost > 0) ((totalAmount / totalTicketCost) * 100).toFloat() else 0f
}

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val venueId: String,
    val venueName: String,
    val category: String,
    val savedAmount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC")
    fun getAllCheckIns(): Flow<List<CheckInEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(checkIns: List<CheckInEntity>)

    @Insert
    suspend fun insertCheckIn(checkIn: CheckInEntity)

    @Delete
    suspend fun deleteCheckIn(checkIn: CheckInEntity)

    @Query("SELECT SUM(savedAmount) FROM check_ins")
    fun getTotalSaved(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM check_ins WHERE venueId = :venueId AND timestamp BETWEEN :startOfDay AND :endOfDay")
    suspend fun countVisitsForVenueOnDay(venueId: String, startOfDay: Long, endOfDay: Long): Int
}

@Database(entities = [CheckInEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checkInDao(): CheckInDao
}