package com.example.freizeittickettracker.domain

import com.example.freizeittickettracker.data.TarifCategory
import com.example.freizeittickettracker.data.model.PersonProfile

object FamilyTarifEngine {

    fun resolveCategory(birthYear: Int, season: String): TarifCategory {
        val startYear = season.split("/").firstOrNull()?.toIntOrNull() ?: 2026
        val age = startYear - birthYear

        return when {
            age <= 13 -> TarifCategory.KIND
            age in 14..16 -> TarifCategory.JUGEND
            age in 17..21 -> TarifCategory.U23
            age >= 65 -> TarifCategory.SENIOREN
            else -> TarifCategory.ERWACHSENE
        }
    }

    fun calculateTicketPrice(
        person: PersonProfile,
        season: String,
        childIndexInFamily: Int = 0
    ): Double {
        if (person.customTicketPrice != null) return person.customTicketPrice

        val cat = resolveCategory(person.birthYear, season)
        if (person.isFamilyPackageApplied && cat == TarifCategory.KIND && childIndexInFamily >= 3) {
            return 0.0
        }

        return if (person.isEarlyBird) cat.earlyBirdPrice else cat.normalPrice
    }
}