
fun getSeasonForDate(date: java.time.LocalDate): String {
    val year = date.year
    return if (date.monthValue >= 10) {
        "$year/${year + 1}"
    } else {
        "${year - 1}/$year"
    }
}