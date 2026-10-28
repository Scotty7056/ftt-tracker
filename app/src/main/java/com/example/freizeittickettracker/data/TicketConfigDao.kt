package com.example.freizeittickettracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketConfigDao {
    @Query("SELECT * FROM ticket_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<TicketConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: TicketConfig)
}