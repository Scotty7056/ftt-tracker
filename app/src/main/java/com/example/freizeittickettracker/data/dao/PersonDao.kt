package com.example.freizeittickettracker.data.dao

import androidx.room.*
import com.example.freizeittickettracker.data.model.PersonProfile
import com.example.freizeittickettracker.data.model.VisitPersonCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY sortOrder ASC, id ASC")
    fun getAllPersons(): Flow<List<PersonProfile>>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: Long): PersonProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonProfile): Long

    @Update
    suspend fun updatePerson(person: PersonProfile)

    @Delete
    suspend fun deletePerson(person: PersonProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitPersonCrossRef(crossRef: VisitPersonCrossRef)

    @Query("DELETE FROM visit_person_cross_ref WHERE visitId = :visitId")
    suspend fun deleteCrossRefsForVisit(visitId: Long)

    @Query("SELECT personId FROM visit_person_cross_ref WHERE visitId = :visitId")
    suspend fun getPersonIdsForVisit(visitId: Long): List<Long>
    @Query("SELECT * FROM visit_person_cross_ref")
    fun getAllCrossRefsFlow(): Flow<List<VisitPersonCrossRef>>
}