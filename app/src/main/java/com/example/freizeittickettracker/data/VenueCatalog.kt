package com.example.freizeittickettracker.data

object VenueCatalog {
    val venues: List<Venue> = listOf(
        // ==========================================
        // 1. BERGBAHNEN & SKIGEBIETE (INNSBRUCK & UMGEBUNG / TIROL)
        // ==========================================
        Venue("ski_nordkette", "Innsbrucker Nordkettenbahnen", VenueCategory.SKI, 47.2865, 11.3989, 48.0, 1200),
        Venue("ski_patscherkofel", "Patscherkofelbahn Igls", VenueCategory.SKI, 47.2272, 11.4132, 45.0, 1000),
        Venue("ski_muttereralm", "Muttereralm Götzens/Mutters", VenueCategory.SKI, 47.2268, 11.3725, 42.5, 900),
        Venue("ski_axamer_lizum", "Axamer Lizum", VenueCategory.SKI, 47.1958, 11.3025, 54.0, 1500),
        Venue("ski_kuehtai", "Kühtai Bergbahnen", VenueCategory.SKI, 47.2140, 11.0205, 52.0, 1200),
        Venue("ski_schlick2000", "Schlick 2000 (Fulpmes)", VenueCategory.SKI, 47.1538, 11.3482, 53.0, 1200),
        Venue("ski_stubai_glacier", "Stubaier Gletscher", VenueCategory.SKI, 47.0105, 11.1578, 67.0, 2000),
        Venue("ski_serles", "Serlesbahnen Mieders", VenueCategory.SKI, 47.1385, 11.3855, 38.0, 800),
        Venue("ski_elfer", "Elferbahnen Neustift", VenueCategory.SKI, 47.1082, 11.3072, 39.0, 800),
        Venue("ski_glungezer", "Glungezerbahn Tulfes", VenueCategory.SKI, 47.2642, 11.5325, 43.0, 900),
        Venue("ski_bergeralm", "Bergeralm Steinach am Brenner", VenueCategory.SKI, 47.0867, 11.4674, 48.0, 1000),
        Venue("ski_rangger_koepfl", "Oberperfuss Rangger Köpfl", VenueCategory.SKI, 47.2618, 11.2394, 38.0, 800),
        Venue("ski_seefeld_rosshuette", "Rosshütte Seefeld", VenueCategory.SKI, 47.3326, 11.1965, 56.5, 1100),
        Venue("ski_seefeld_gschwandtkopf", "Gschwandtkopf Seefeld", VenueCategory.SKI, 47.3235, 11.1798, 46.0, 900),
        Venue("ski_hochmuttersberg", "Kellerjochbahn Schwaz", VenueCategory.SKI, 47.3411, 11.6668, 39.0, 800),
        Venue("ski_spieljoch", "Spieljochbahn Fügen (Zillertal)", VenueCategory.SKI, 47.3436, 11.8398, 65.0, 1400),
        Venue("ski_hochzillertal", "Hochzillertal Kaltenbach", VenueCategory.SKI, 47.2905, 11.8741, 69.5, 1500),
        Venue("ski_hochfuegen", "Hochfügen", VenueCategory.SKI, 47.2685, 11.7765, 69.5, 1500),
        Venue("ski_achensee_rofan", "Rofan Seilbahn Maurach", VenueCategory.SKI, 47.4241, 11.7588, 48.0, 1000),
        Venue("ski_achensee_karwendel", "Karwendelbahn Pertisau", VenueCategory.SKI, 47.4382, 11.6934, 42.0, 900),
        Venue("ski_achensee_christlum", "Hochalmlifte Christlum Achenkirch", VenueCategory.SKI, 47.5305, 11.7012, 49.0, 1100),
        Venue("ski_wildschoenau_markbach", "Markbachjoch Niederau (Wildschönau)", VenueCategory.SKI, 47.4308, 12.0465, 58.0, 1200),
        Venue("ski_wildschoenau_schatzberg", "Schatzberg Auffach (Ski Juwel)", VenueCategory.SKI, 47.3892, 11.9961, 62.0, 1300),
        Venue("ski_alpbachtal", "Wiedersbergerhorn Alpbach", VenueCategory.SKI, 47.3985, 11.9421, 62.0, 1300),
        Venue("ski_hohe_salve_hopfgarten", "SkiWelt Hopfgarten (Hohe Salve)", VenueCategory.SKI, 47.4475, 12.1612, 69.0, 1600),
        Venue("ski_hohe_salve_itler", "SkiWelt Itter", VenueCategory.SKI, 47.4682, 12.1465, 69.0, 1600),
        Venue("ski_steinplatte", "Steinplatte Waidring", VenueCategory.SKI, 47.5815, 12.5712, 63.0, 1500),
        Venue("ski_st_johann", "Bergbahnen St. Johann in Tirol", VenueCategory.SKI, 47.5192, 12.4345, 59.0, 1300),
        Venue("ski_fieberbrunn", "Bergbahnen Fieberbrunn", VenueCategory.SKI, 47.4642, 12.5492, 72.0, 1500),
        Venue("ski_buchensteinwand", "Buchensteinwand Pillersee", VenueCategory.SKI, 47.5022, 12.5785, 46.0, 900),
        Venue("ski_ehrenbuerg", "Ehrenbürg / Pfronten-Zugspitzblick", VenueCategory.SKI, 47.5255, 10.5841, 38.0, 800),

        // ==========================================
        // 2. HALLENBÄDER & HALLEN-WELLNESS
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

        // ==========================================
        // 3. FREIBÄDER & BADESEEN
        // ==========================================
        Venue("pool_tivoli_freibad", "Tivoli Freibad Innsbruck", VenueCategory.POOL, 47.2585, 11.4112, 6.8, 500),
        Venue("lake_baggersee_rossau", "Baggersee Rossau Innsbruck", VenueCategory.POOL, 47.2691, 11.4585, 6.0, 600),
        Venue("lake_natters", "Natterer See", VenueCategory.POOL, 47.2341, 11.3412, 6.5, 500),
        Venue("lake_lans", "Lanser See", VenueCategory.POOL, 47.2405, 11.4289, 6.5, 400),
        Venue("pool_hall_freibad", "Freischwimmbad Hall in Tirol", VenueCategory.POOL, 47.2805, 11.5075, 6.5, 450),
        Venue("pool_wattens_freibad", "Alpenbad Wattens", VenueCategory.POOL, 47.2891, 11.5932, 6.5, 450),
        Venue("pool_schwaz_freibad", "Erlebnisfreibad Schwaz", VenueCategory.POOL, 47.3465, 11.7051, 6.0, 450),
        Venue("lake_weissensee_tirol", "Badesee Weißlahn Terfens", VenueCategory.POOL, 47.3235, 11.6421, 5.5, 500),
        Venue("pool_zirl_freibad", "Schwimmbad Zirl", VenueCategory.POOL, 47.2712, 11.2415, 5.5, 400),
        Venue("pool_telfs_freibad", "Telfer Bad (Freibadbereich)", VenueCategory.POOL, 47.3055, 11.0720, 7.0, 450),

        // ==========================================
        // 4. EISSPORT & EISLAUFPLÄTZE
        // ==========================================
        Venue("ice_olympiaworld", "Olympiaworld Innsbruck (Eissportzentrum)", VenueCategory.ICE, 47.2575, 11.4101, 7.0, 400),
        Venue("ice_goetzens", "Eissportzentrum Götzens", VenueCategory.ICE, 47.2351, 11.3121, 5.5, 350),
        Venue("ice_telfs", "SportZentrum Telfs Eishalle", VenueCategory.ICE, 47.3082, 11.0665, 6.0, 350),
        Venue("ice_kufstein", "Eisarena Kufstein", VenueCategory.ICE, 47.5812, 12.1645, 6.5, 400),

        // ==========================================
        // 5. KULTUR, MUSEEN & AUSFLUGSZIELE
        // ==========================================
        Venue("cult_alpenzoo", "Alpenzoo Innsbruck", VenueCategory.CULTURE, 47.2818, 11.3985, 14.0, 400),
        Venue("cult_bergisel", "Bergiselschanze Innsbruck", VenueCategory.CULTURE, 47.2488, 11.3995, 11.0, 350),
        Venue("cult_ferdinandeum", "Tiroler Landesmuseum Ferdinandeum", VenueCategory.CULTURE, 47.2675, 11.3972, 12.0, 300),
        Venue("cult_zeughaus", "Museum im Zeughaus Innsbruck", VenueCategory.CULTURE, 47.2701, 11.4085, 8.0, 300),
        Venue("cult_volkskunstmuseum", "Tiroler Volkskunstmuseum Innsbruck", VenueCategory.CULTURE, 47.2685, 11.3955, 12.0, 300),
        Venue("cult_tirol_panorama", "Das Tirol Panorama mit Kaiserjägermuseum", VenueCategory.CULTURE, 47.2505, 11.4012, 12.0, 350),
        Venue("cult_schloss_ambras", "Schloss Ambras Innsbruck", VenueCategory.CULTURE, 47.2562, 11.4342, 16.0, 500),
        Venue("cult_muenze_hall", "Burg Hasegg / Münze Hall", VenueCategory.CULTURE, 47.2795, 11.5065, 11.5, 300),
        Venue("cult_silberbergwerk", "Schwazer Silberbergwerk", VenueCategory.CULTURE, 47.3512, 11.7225, 19.0, 400)
    )

    fun findById(id: String): Venue? = venues.firstOrNull { it.id == id }
}