package com.example.freizeittickettracker.data.weather

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OpenMeteoResponse(
    val current: CurrentUnits? = null,
    val daily: DailyUnits? = null
)

@Serializable
data class CurrentUnits(
    @SerialName("temperature_2m") val temperature: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0,
    @SerialName("wind_speed_10m") val windSpeed: Double = 0.0
)

@Serializable
data class DailyUnits(
    @SerialName("temperature_2m_max") val tempMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val tempMin: List<Double> = emptyList(),
    @SerialName("snowfall_sum") val snowfallSum: List<Double> = emptyList()
)

data class VenueWeather(
    val currentTemp: Double,
    val weatherDescription: String,
    val windSpeedKmh: Double,
    val todaySnowfallCm: Double
)

class WeatherRepository {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getVenueWeather(lat: Double, lon: Double): Result<VenueWeather> = runCatching {
        val res: OpenMeteoResponse = client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", lat)
            parameter("longitude", lon)
            parameter("current", "temperature_2m,weather_code,wind_speed_10m")
            parameter("daily", "temperature_2m_max,temperature_2m_min,snowfall_sum")
            parameter("timezone", "auto")
            parameter("forecast_days", 1)
        }.body()

        val cur = res.current ?: throw IllegalStateException("Keine Wetterdaten")
        VenueWeather(
            currentTemp = cur.temperature,
            weatherDescription = when (cur.weatherCode) {
                0 -> "Klar / Sonnig"
                1, 2, 3 -> "Heiter bis wolkig"
                71, 73, 75, 85, 86 -> "Schneefall"
                else -> "BewÃ¶lkt"
            },
            windSpeedKmh = cur.windSpeed,
            todaySnowfallCm = res.daily?.snowfallSum?.firstOrNull() ?: 0.0
        )
    }
}