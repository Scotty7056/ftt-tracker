package com.example.freizeittickettracker.data

import kotlinx.coroutines.flow.Flow

class TrackerRepository(
    private val visitDao: VisitDao,
    private val ticketConfigDao: TicketConfigDao
) {
    val allVisits: Flow<List<VisitEntity>> = visitDao.getAllVisits()

    val ticketConfigFlow: Flow<TicketConfig> = ticketConfigDao.getConfigFlow()

    suspend fun insertVisit(visit: VisitEntity) {
        visitDao.insertVisit(visit)
    }

    suspend fun updateVisit(visit: VisitEntity) {
        visitDao.updateVisit(visit)
    }

    suspend fun deleteVisit(visit: VisitEntity) {
        visitDao.deleteVisit(visit)
    }

    suspend fun saveTicketConfig(config: TicketConfig) {
        ticketConfigDao.saveConfig(config)
    }
}