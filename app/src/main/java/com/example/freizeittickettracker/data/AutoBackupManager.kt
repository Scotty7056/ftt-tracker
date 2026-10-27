package com.example.freizeittickettracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AutoBackupManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun performAutoBackup(context: Context, visits: List<VisitEntity>) = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val latestFile = File(backupDir, "ftt_autobackup_latest.json")
            latestFile.writeText(json.encodeToString(visits))

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val archiveFile = File(backupDir, "ftt_backup_$timestamp.json")
            archiveFile.writeText(json.encodeToString(visits))

            // Maximal die letzten 5 Backups aufbewahren
            val files = backupDir.listFiles { _, name -> name.startsWith("ftt_backup_") } ?: emptyArray()
            if (files.size > 5) {
                files.sortedBy { it.lastModified() }.take(files.size - 5).forEach { it.delete() }
            }
        } catch (_: Exception) {}
    }

    suspend fun exportCsv(visits: List<VisitEntity>): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        sb.append("ID;Datum;Saison;Kategorie;Gebiet;Ersparnis_EUR;Notiz\n")
        visits.forEach {
            sb.append("${it.id};${it.date};${it.season};${it.category};\"${it.venueName}\";${it.savedAmount};\"${it.notes}\"\n")
        }
        sb.toString()
    }

    fun parseCsvToVisits(csvContent: String): List<VisitEntity> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val parsedVisits = mutableListOf<VisitEntity>()
        val header = lines.first().lowercase()
        val dataLines = if (header.contains("datum") || header.contains("date") || header.contains("venue")) lines.drop(1) else lines

        for (line in dataLines) {
            val delimiter = if (line.contains(";")) ";" else ","
            val tokens = line.split(delimiter).map { it.trim().removeSurrounding("\"") }
            if (tokens.size < 2) continue

            val rawDate = tokens[0]
            val parsedDate = try {
                if (rawDate.contains(".")) {
                    val parts = rawDate.split(".")
                    val day = parts[0].padStart(2, '0')
                    val month = parts[1].padStart(2, '0')
                    val year = if (parts[2].length == 2) "20${parts[2]}" else parts[2]
                    java.time.LocalDate.parse("$year-$month-$day")
                } else {
                    java.time.LocalDate.parse(rawDate)
                }
            } catch (_: Exception) {
                continue
            }

            val venueName = tokens[1]
            val matchedVenue = VenueCatalog.venues.find { 
                it.name.equals(venueName, ignoreCase = true) || it.name.contains(venueName, ignoreCase = true)
            }

            val category = if (tokens.size > 2 && tokens[2].isNotBlank()) {
                tokens[2].uppercase()
            } else {
                matchedVenue?.category?.name ?: VenueCategory.SKI.name
            }

            val amount = if (tokens.size > 3 && tokens[3].isNotBlank()) {
                tokens[3].replace("€", "").replace(",", ".").trim().toDoubleOrNull() ?: (matchedVenue?.normalTicketPrice ?: 0.0)
            } else {
                matchedVenue?.normalTicketPrice ?: 0.0
            }

            val notes = if (tokens.size > 4) tokens.drop(4).joinToString(" ") else "CSV Import"
            val season = getSeasonForDate(parsedDate)

            parsedVisits.add(
                VisitEntity(
                    venueId = matchedVenue?.id ?: venueName.lowercase().replace(" ", "_"),
                    venueName = matchedVenue?.name ?: venueName,
                    category = category,
                    date = parsedDate,
                    season = season,
                    savedAmount = amount,
                    notes = notes
                )
            )
        }
        return parsedVisits
    }
}