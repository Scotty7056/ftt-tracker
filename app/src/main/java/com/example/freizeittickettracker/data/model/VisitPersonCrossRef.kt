package com.example.freizeittickettracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.freizeittickettracker.data.VisitEntity

@Entity(
    tableName = "visit_person_cross_ref",
    primaryKeys = ["visitId", "personId"],
    foreignKeys = [
        ForeignKey(
            entity = VisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonProfile::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId"), Index("visitId")]
)
data class VisitPersonCrossRef(
    val visitId: Long,
    val personId: Long
)