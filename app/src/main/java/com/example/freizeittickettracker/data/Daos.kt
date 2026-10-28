package com.example.freizeittickettracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY date DESC")
    fun getAllVisits(): Flow<List<VisitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Delete
    suspend fun deleteVisit(visit: VisitEntity)
}

@Dao
interface TicketConfigDao {
    @Query("SELECT * FROM ticket_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<TicketConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: TicketConfigEntity)
}