
fun getSeasonForDate(date: java.time.LocalDate): String {
    val year = date.year
    return if (date.monthValue >= 10) {
        "$year/${year + 1}"
    } else {
        "${year - 1}/$year"
    }

    fun checkAutoPriceSync() {
        viewModelScope.launch {
            PriceSyncManager.syncPrices(getApplication(), force = false)
        }
    }

    fun manualPriceSync(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = PriceSyncManager.syncPrices(getApplication(), force = true)
            result.onSuccess { count ->
                onResult(true, "$count Tarife erfolgreich aktualisiert!")
            }.onFailure { err ->
                onResult(false, "Fehler beim Laden: ${err.localizedMessage}")
            }
        }
    }
}