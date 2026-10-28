package com.example.freizeittickettracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackerRepository(
    private val visitDao: VisitDao,
    private val ticketConfigDao: TicketConfigDao
) {
    val allVisits: Flow<List<VisitEntity>> = visitDao.getAllVisits()

    val ticketConfigFlow: Flow<TicketConfig> = ticketConfigDao.getConfig().map { entity ->
        if (entity != null) {
            TicketConfig(
                category = try { TarifCategory.valueOf(entity.category) } catch (_: Exception) { TarifCategory.ADULT },
                isEarlyBird = entity.isEarlyBird
            )
        } else {
            TicketConfig()
        }
    }

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
        ticketConfigDao.saveConfig(
            TicketConfigEntity(
                id = 1,
                category = config.category.name,
                isEarlyBird = config.isEarlyBird
            )
        )
    }
}