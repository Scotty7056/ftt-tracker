package com.example.freizeittickettracker.data

import android.content.Context
import androidx.preference.PreferenceManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

@Serializable
data class PriceSyncPayload(
    val version: Int = 1,
    val updated_at: String = "",
    val prices: Map<String, Double> = emptyMap()
)

object PriceSyncManager {
    // URL zu deiner Raw-JSON auf GitHub:
    private const val PRICE_URL = "https://raw.githubusercontent.com/scotty7056/ftt-tracker/main/docs/prices.json"
    private const val PREF_LAST_SYNC = "pref_last_price_sync_timestamp"
    private const val SYNC_INTERVAL_DAYS = 14L

    fun getCustomPrice(context: Context, venueId: String, fallbackPrice: Double): Double {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = "venue_price_$venueId"
        return if (prefs.contains(key)) {
            prefs.getFloat(key, fallbackPrice.toFloat()).toDouble()
        } else {
            fallbackPrice
        }
    }

    suspend fun syncPrices(context: Context, force: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lastSync = prefs.getLong(PREF_LAST_SYNC, 0L)
        val now = System.currentTimeMillis()
        val daysSinceLastSync = TimeUnit.MILLISECONDS.toDays(now - lastSync)

        if (!force && daysSinceLastSync < SYNC_INTERVAL_DAYS && lastSync != 0L) {
            return@withContext Result.success(0) // Kein Update nötig
        }

        try {
            val client = HttpClient(Android) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val payload: PriceSyncPayload = client.get(PRICE_URL).body()
            client.close()

            val editor = prefs.edit()
            payload.prices.forEach { (venueId, price) ->
                editor.putFloat("venue_price_$venueId", price.toFloat())
            }
            editor.putLong(PREF_LAST_SYNC, now)
            editor.apply()

            Result.success(payload.prices.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}