package com.example.freizeittickettracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM ticket_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<TicketConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: TicketConfigEntity)
}