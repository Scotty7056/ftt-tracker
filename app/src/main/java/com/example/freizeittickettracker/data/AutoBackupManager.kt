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
}