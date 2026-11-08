package com.example.freizeittickettracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val birthYear: Int,
    val customTicketPrice: Double? = null,
    val isEarlyBird: Boolean = true,
    val isFamilyPackageApplied: Boolean = false,
    val isDefaultSelected: Boolean = true,
    val sortOrder: Int = 0
)