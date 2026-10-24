package com.example.freizeittickettracker.data

val skiVenues = listOf(
    Venue("axamer_lizum", "Axamer Lizum", Category.SKI, 64.0, 57.0, 51.0, 32.0, 47.1956, 11.3025, 2000.0, "https://panodata.panomax.com/cams/381/recent_default.jpg", "https://axamer-lizum.panomax.com/hoadl"),
    Venue("nordkette", "Innsbrucker Nordkettenbahnen", Category.SKI, 52.0, 47.0, 42.0, 26.0, 47.2861, 11.3986, 1200.0, "https://panodata.panomax.com/cams/195/recent_default.jpg", "https://nordkette.panomax.com/hafelekar"),
    Venue("patscherkofel", "Patscherkofelbahn Igls", Category.SKI, 49.50, 44.50, 40.0, 25.0, 47.2297, 11.4137, 1200.0, "https://panodata.panomax.com/cams/1190/recent_default.jpg", "https://patscherkofel.panomax.com/"),
    Venue("kuehtai", "Bergbahnen KÃ¼htai", Category.SKI, 56.0, 50.0, 45.0, 28.0, 47.2131, 11.0203, 2500.0, "https://panodata.panomax.com/cams/460/recent_default.jpg", "https://kuehtai.panomax.com/"),
    Venue("stubai_glacier", "Stubaier Gletscher", Category.SKI, 72.0, 65.0, 58.0, 36.0, 47.0097, 11.1542, 2500.0, "https://panodata.panomax.com/cams/257/recent_default.jpg", "https://stubaier-gletscher.panomax.com/eisgrat"),
    Venue("schlick_2000", "Schlick 2000 Fulpmes", Category.SKI, 56.50, 51.0, 45.0, 28.50, 47.1519, 11.3411, 1500.0)
)

val poolVenues = listOf(
    Venue("ikb_amraser", "Hallenbad Amraser StraÃŸe", Category.SWIMMING, 9.50, 7.50, 6.50, 4.80, 47.2628, 11.4081, 300.0),
    Venue("ikb_odorf", "Hallenbad O-Dorf", Category.SWIMMING, 12.00, 9.50, 8.00, 6.00, 47.2711, 11.4428, 400.0),
    Venue("ikb_tivoli", "Freibad Tivoli", Category.SWIMMING, 7.50, 5.50, 5.00, 3.50, 47.2589, 11.4111, 500.0)
)

val lakeVenues = listOf(
    Venue("baggersee_rossau", "Badesee Rossau (Baggersee)", Category.LAKE, 6.00, 4.50, 4.00, 3.00, 47.2655, 11.4469, 600.0),
    Venue("natterer_see", "Ferienparadies Natterer See", Category.LAKE, 7.50, 6.00, 5.00, 3.80, 47.2344, 11.3481, 500.0)
)

val cultureVenues = listOf(
    Venue("ferdinandeum", "Tiroler Landesmuseum Ferdinandeum", Category.CULTURE, 14.00, 10.00, 10.00, 0.00, 47.2673, 11.3967, 250.0),
    Venue("schloss_ambras", "Schloss Ambras Innsbruck", Category.CULTURE, 16.00, 12.00, 12.00, 0.00, 47.2562, 11.4344, 400.0)
)

object VenueCatalog {
    val venues: List<Venue> = listOf(skiVenues, poolVenues, lakeVenues, cultureVenues).flatten()
}