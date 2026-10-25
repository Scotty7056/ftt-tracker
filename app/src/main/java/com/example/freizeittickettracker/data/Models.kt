package com.example.freizeittickettracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

fun Double.formatEuro(): String = "%.2f €".format(this)

enum class VenueCategory(val displayName: String, val iconLabel: String) {
    SKI("Skigebiet / Bergbahn", "⛷️ Ski"),
    SLED("Rodelbahn", "🛷 Rodeln"),
    ICE("Eislaufen / Eissport", "⛸️ Eislaufen"),
    POOL("Hallenbad / Badesee", "🏊 Bäder"),
    CULTURE("Kultur & Sehenswürdigkeit", "🏛️ Kultur")
}

data class Venue(
    val id: String,
    val name: String,
    val category: VenueCategory,
    val latitude: Double,
    val longitude: Double,
    val normalTicketPrice: Double,
    val geofenceRadiusMeters: Int = 1000,
    val subtitle: String = ""
)

enum class TarifCategory(val label: String, val normalPrice: Double, val earlyBirdPrice: Double) {
    ADULT("Erwachsene (1962–2002)", 995.0, 915.0),
    U23("U23 (2003–2007)", 765.0, 710.0),
    YOUTH("Jugend (2008–2010)", 595.0, 550.0),
    CHILD("Kinder (ab 2011)", 315.0, 290.0),
    SENIOR("Senioren (ab 1961)", 845.0, 780.0),
    HANDICAPPED("Menschen mit Behinderung", 695.0, 640.0)
}

data class TicketConfig(
    val category: TarifCategory = TarifCategory.U23,
    val isEarlyBird: Boolean = true
) {
    fun getPrice(): Double = if (isEarlyBird) category.earlyBirdPrice else category.normalPrice
    fun toEntity(): TicketConfigEntity = TicketConfigEntity(
        id = 1,
        categoryName = category.name,
        isEarlyBird = isEarlyBird
    )
}

@Entity(tableName = "ticket_config")
data class TicketConfigEntity(
    @PrimaryKey val id: Int = 1,
    val categoryName: String,
    val isEarlyBird: Boolean
) {
    fun toModel(): TicketConfig {
        val cat = try {
            TarifCategory.valueOf(categoryName)
        } catch (e: Exception) {
            TarifCategory.U23
        }
        return TicketConfig(category = cat, isEarlyBird = isEarlyBird)
    }
}

@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val venueId: String,
    val venueName: String,
    val category: String,
    val date: LocalDate,
    val savedAmount: Double,
    val notes: String = ""
)

data class DashboardStats(
    val tarifTitle: String = "U23 (Vorverkauf)",
    val purchasePrice: Double = 710.0,
    val totalValueSaved: Double = 0.0,
    val remainingToAmortize: Double = 710.0,
    val netSavings: Double = 0.0,
    val amortizationProgress: Float = 0f,
    val isAmortized: Boolean = false,
    val totalVisits: Int = 0
)