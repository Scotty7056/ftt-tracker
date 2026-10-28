package com.example.freizeittickettracker.data

import android.content.Context
import java.io.File
import java.time.LocalDate

object AutoBackupManager {
    fun exportCsv(visits: List<VisitEntity>): String {
        val sb = StringBuilder()
        sb.append("Datum;Gebiet;Kategorie;Ersparnis;Saison;Notizen\n")
        for (v in visits) {
            sb.append("${v.date};${v.venueName};${v.category};${v.savedAmount};${v.season};${v.notes}\n")
        }
        return sb.toString()
    }

    fun performAutoBackup(context: Context, visits: List<VisitEntity>) {
        try {
            val file = File(context.filesDir, "ftt_auto_backup.csv")
            file.writeText(exportCsv(visits))
        } catch (_: Exception) {}
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
                    LocalDate.parse("$year-$month-$day")
                } else {
                    LocalDate.parse(rawDate)
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

            val notes = if (tokens.size > 5) {
                tokens.drop(5).joinToString(" ")
            } else if (tokens.size == 5 && !tokens[4].contains("/")) {
                tokens[4]
            } else {
                "CSV Import"
            }

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