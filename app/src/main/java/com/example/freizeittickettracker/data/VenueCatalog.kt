package com.example.freizeittickettracker.data

object VenueCatalog {
    val venues: List<Venue> = listOf(
        // ==========================================
        // 1. INNSBRUCK & UMGEBUNG
        // ==========================================
        Venue("ski_nordkette", "Innsbrucker Nordkettenbahnen", VenueCategory.SKI, 47.2865, 11.3989, 48.0, 1200),
        Venue("ski_patscherkofel", "Patscherkofelbahn Igls", VenueCategory.SKI, 47.2272, 11.4132, 45.0, 1000),
        Venue("ski_muttereralm", "Muttereralm Götzens/Mutters", VenueCategory.SKI, 47.2268, 11.3725, 42.5, 900),
        Venue("ski_axamer_lizum", "Axamer Lizum", VenueCategory.SKI, 47.1958, 11.3025, 54.0, 1500),
        Venue("ski_kuehtai", "Kühtai Bergbahnen", VenueCategory.SKI, 47.2140, 11.0205, 52.0, 1200),
        Venue("ski_rangger_koepfl", "Oberperfuss Rangger Köpfl", VenueCategory.SKI, 47.2618, 11.2394, 38.0, 800),
        Venue("ski_glungezer", "Glungezerbahn Tulfes", VenueCategory.SKI, 47.2642, 11.5325, 43.0, 900),
        Venue("ski_seefeld_rosshuette", "Rosshütte Seefeld", VenueCategory.SKI, 47.3326, 11.1965, 56.5, 1100),
        Venue("ski_seefeld_gschwandtkopf", "Gschwandtkopf Seefeld", VenueCategory.SKI, 47.3235, 11.1798, 46.0, 900),

        // ==========================================
        // 2. STUBAITAL & WIPPTAL
        // ==========================================
        Venue("ski_schlick2000", "Schlick 2000 (Fulpmes)", VenueCategory.SKI, 47.1538, 11.3482, 53.0, 1200),
        Venue("ski_stubai_glacier", "Stubaier Gletscher", VenueCategory.SKI, 47.0105, 11.1578, 67.0, 2000),
        Venue("ski_serles", "Serlesbahnen Mieders", VenueCategory.SKI, 47.1385, 11.3855, 38.0, 800),
        Venue("ski_elfer", "Elferbahnen Neustift", VenueCategory.SKI, 47.1082, 11.3072, 39.0, 800),
        Venue("ski_bergeralm", "Bergeralm Steinach am Brenner", VenueCategory.SKI, 47.0867, 11.4674, 48.0, 1000),

        // ==========================================
        // 3. OBERLAND, PAZNAUN & ARLBERG
        // ==========================================
        Venue("ski_ischgl", "Silvretta Arena Ischgl / Samnaun", VenueCategory.SKI, 47.0118, 10.2912, 76.0, 2000),
        Venue("ski_arlberg_stanton", "Ski Arlberg (St. Anton / St. Christoph)", VenueCategory.SKI, 47.1296, 10.2685, 78.0, 2500),
        Venue("ski_arlberg_lech", "Ski Arlberg (Lech / Zürs / Warth)", VenueCategory.SKI, 47.2104, 10.1425, 78.0, 2500),
        Venue("ski_serfaus_fiss_ladis", "Serfaus-Fiss-Ladis", VenueCategory.SKI, 47.0385, 10.6092, 72.0, 2000),
        Venue("ski_soelden", "Sölden (Ötztal)", VenueCategory.SKI, 46.9678, 11.0075, 74.0, 2000),
        Venue("ski_gurgl", "Obergurgl - Hochgurgl", VenueCategory.SKI, 46.8712, 11.0265, 71.0, 1800),
        Venue("ski_hochoetz", "Hochoetz (Ötz)", VenueCategory.SKI, 47.2025, 10.9015, 52.0, 1200),
        Venue("ski_pitztal_gletscher", "Pitztaler Gletscher & Rifflsee", VenueCategory.SKI, 46.9245, 10.8715, 68.0, 1800),
        Venue("ski_hochzeiger", "Hochzeiger Jerzens (Pitztal)", VenueCategory.SKI, 47.1455, 10.7485, 55.0, 1200),
        Venue("ski_kaunertal", "Kaunertaler Gletscher & Fendels", VenueCategory.SKI, 46.9255, 10.7225, 61.0, 1800),
        Venue("ski_venet", "Venet Bergbahnen Landeck/Zams", VenueCategory.SKI, 47.1565, 10.5912, 49.0, 1100),
        Venue("ski_galtuer", "Silvapark Galtür", VenueCategory.SKI, 46.9685, 10.1875, 57.0, 1200),
        Venue("ski_kappl", "Bergbahnen Kappl", VenueCategory.SKI, 47.0612, 10.3725, 55.0, 1200),
        Venue("ski_see", "Bergbahnen See (Paznaun)", VenueCategory.SKI, 47.0815, 10.4692, 53.0, 1100),
        Venue("ski_ehrwalder_alm", "Ehrwalder Alm & Zugspitzbahn", VenueCategory.SKI, 47.3985, 10.9325, 58.5, 1400),
        Venue("ski_lermoos", "Lermoos Grubigstein", VenueCategory.SKI, 47.3995, 10.8812, 56.5, 1200),
        Venue("ski_berwang", "Berwang Skischaukel", VenueCategory.SKI, 47.4085, 10.7455, 54.0, 1100),
        Venue("ski_tannheim", "Tannheimer Tal (Neunerköpfle / Füssener Jöchle)", VenueCategory.SKI, 47.4985, 10.5185, 49.0, 1200),

        // ==========================================
        // 4. UNTERLAND, ZILLERTAL & KITZBÜHELER ALPEN
        // ==========================================
        Venue("ski_kellerjoch", "Kellerjochbahn Schwaz", VenueCategory.SKI, 47.3411, 11.6668, 39.0, 800),
        Venue("ski_spieljoch", "Spieljochbahn Fügen (Zillertal)", VenueCategory.SKI, 47.3436, 11.8398, 65.0, 1400),
        Venue("ski_hochzillertal", "Hochzillertal Kaltenbach", VenueCategory.SKI, 47.2905, 11.8741, 69.5, 1500),
        Venue("ski_hochfuegen", "Hochfügen", VenueCategory.SKI, 47.2685, 11.7765, 69.5, 1500),
        Venue("ski_zillertal_arena", "Zillertal Arena (Zell / Gerlos)", VenueCategory.SKI, 47.2341, 11.9025, 72.5, 1800),
        Venue("ski_mayrhofen", "Mayrhofner Bergbahnen (Penken / Ahorn)", VenueCategory.SKI, 47.1685, 11.8625, 72.5, 1800),
        Venue("ski_hintertux", "Hintertuxer Gletscher", VenueCategory.SKI, 47.1085, 11.6755, 72.5, 2000),
        Venue("ski_achensee_rofan", "Rofan Seilbahn Maurach", VenueCategory.SKI, 47.4241, 11.7588, 48.0, 1000),
        Venue("ski_achensee_karwendel", "Karwendelbahn Pertisau", VenueCategory.SKI, 47.4382, 11.6934, 42.0, 900),
        Venue("ski_achensee_christlum", "Hochalmlifte Christlum Achenkirch", VenueCategory.SKI, 47.5305, 11.7012, 49.0, 1100),
        Venue("ski_wildschoenau_markbach", "Markbachjoch Niederau (Wildschönau)", VenueCategory.SKI, 47.4308, 12.0465, 58.0, 1200),
        Venue("ski_wildschoenau_schatzberg", "Schatzberg Auffach (Ski Juwel)", VenueCategory.SKI, 47.3892, 11.9961, 62.0, 1300),
        Venue("ski_alpbachtal", "Wiedersbergerhorn Alpbach", VenueCategory.SKI, 47.3985, 11.9421, 62.0, 1300),
        Venue("ski_skiwelt_hopfgarten", "SkiWelt Wilder Kaiser - Brixental", VenueCategory.SKI, 47.4475, 12.1612, 69.0, 2000),
        Venue("ski_kitzbuehel", "KitzSki Kitzbühel / Kirchberg", VenueCategory.SKI, 47.4412, 12.3895, 72.0, 2200),
        Venue("ski_st_johann", "Bergbahnen St. Johann in Tirol", VenueCategory.SKI, 47.5192, 12.4345, 59.0, 1300),
        Venue("ski_fieberbrunn", "Bergbahnen Fieberbrunn (Saalbach Verbund)", VenueCategory.SKI, 47.4642, 12.5492, 72.0, 1800),
        Venue("ski_steinplatte", "Steinplatte Waidring", VenueCategory.SKI, 47.5815, 12.5712, 63.0, 1500),
        Venue("ski_buchensteinwand", "Buchensteinwand Pillersee", VenueCategory.SKI, 47.5022, 12.5785, 46.0, 900),
        Venue("ski_koessen", "Unterberg Kössen (Hochkössen)", VenueCategory.SKI, 47.6612, 12.4085, 48.5, 1100),

        // ==========================================
        // 5. OSTTIROL
        // ==========================================
        Venue("ski_grossglockner_kals", "Großglockner Resort Kals-Matrei", VenueCategory.SKI, 47.0012, 12.5412, 59.0, 1500),
        Venue("ski_lienz_zettersfeld", "Lienzer Bergbahnen (Zettersfeld/Hochstein)", VenueCategory.SKI, 46.8455, 12.7812, 54.0, 1200),
        Venue("ski_sillian", "Hochpustertal Sillian Thurntaler", VenueCategory.SKI, 46.7455, 12.4185, 56.0, 1200),
        Venue("ski_st_jakob", "St. Jakob im Defereggental", VenueCategory.SKI, 46.9215, 12.3385, 55.0, 1200),

        // ==========================================
        // 6. BÄDER, SEEN, KULTUR & EIS
        // ==========================================
        Venue("pool_tivoli_hallenbad", "Hallenbad Amraser Straße (Innsbruck)", VenueCategory.POOL, 47.2622, 11.4082, 7.5, 400),
        Venue("pool_olympisches_dorf", "Hallenbad O-Dorf Innsbruck", VenueCategory.POOL, 47.2721, 11.4468, 9.8, 500),
        Venue("pool_hoettinger_au", "Hallenbad Höttinger Au Innsbruck", VenueCategory.POOL, 47.2648, 11.3789, 7.5, 400),
        Venue("pool_telfer_bad", "Telfer Bad (Hallenbad & Sportbad)", VenueCategory.POOL, 47.3051, 11.0715, 11.5, 500),
        Venue("pool_freizeitzentrum_axams", "Freizeitzentrum Axams Hallenbad", VenueCategory.POOL, 47.2285, 11.2785, 10.0, 450),
        Venue("pool_stuBay", "StuBay Freizeitcenter Telfes im Stubai", VenueCategory.POOL, 47.1645, 11.3592, 16.5, 600),
        Venue("pool_wave_woergl", "Wörgler Wasserwelt (Wave)", VenueCategory.POOL, 47.4892, 12.0621, 18.0, 600),
        Venue("pool_hall_in_tirol", "Hallenbad Hall in Tirol", VenueCategory.POOL, 47.2831, 11.5122, 8.0, 400),
        Venue("pool_seefeld_olympia", "Olympiabad Seefeld", VenueCategory.POOL, 47.3278, 11.1832, 18.5, 500),
        Venue("pool_atoll_achensee", "Atoll Achensee (Maurach)", VenueCategory.POOL, 47.4295, 11.7511, 21.0, 600),
        Venue("pool_tivoli_freibad", "Tivoli Freibad Innsbruck", VenueCategory.POOL, 47.2585, 11.4112, 6.8, 500),
        Venue("lake_baggersee_rossau", "Baggersee Rossau Innsbruck", VenueCategory.POOL, 47.2691, 11.4585, 6.0, 600),
        Venue("lake_natters", "Natterer See", VenueCategory.POOL, 47.2341, 11.3412, 6.5, 500),
        Venue("lake_lans", "Lanser See", VenueCategory.POOL, 47.2405, 11.4289, 6.5, 400),
        Venue("pool_hall_freibad", "Freischwimmbad Hall in Tirol", VenueCategory.POOL, 47.2805, 11.5075, 6.5, 450),
        Venue("ice_olympiaworld", "Olympiaworld Innsbruck (Eissportzentrum)", VenueCategory.ICE, 47.2575, 11.4101, 7.0, 400),
        Venue("ice_goetzens", "Eissportzentrum Götzens", VenueCategory.ICE, 47.2351, 11.3121, 5.5, 350),
        Venue("ice_telfs", "SportZentrum Telfs Eishalle", VenueCategory.ICE, 47.3082, 11.0665, 6.0, 350),
        Venue("cult_alpenzoo", "Alpenzoo Innsbruck", VenueCategory.CULTURE, 47.2818, 11.3985, 14.0, 400),
        Venue("cult_bergisel", "Bergiselschanze Innsbruck", VenueCategory.CULTURE, 47.2488, 11.3995, 11.0, 350),
        Venue("cult_ferdinandeum", "Tiroler Landesmuseum Ferdinandeum", VenueCategory.CULTURE, 47.2675, 11.3972, 12.0, 300),
        Venue("cult_schloss_ambras", "Schloss Ambras Innsbruck", VenueCategory.CULTURE, 47.2562, 11.4342, 16.0, 500),
        Venue("cult_silberbergwerk", "Schwazer Silberbergwerk", VenueCategory.CULTURE, 47.3512, 11.7225, 19.0, 400)
    )

    fun findById(id: String): Venue? = venues.firstOrNull { it.id == id }
}