package com.example.freizeittickettracker.data

object VenueCatalog {
    val venues: List<Venue> = listOf(
        // ==========================================
        // 1. BERGBAHNEN & SKIGEBIETE (SKI)
        // ==========================================
        Venue("ski_nordkette", "Innsbrucker Nordkettenbahnen", VenueCategory.SKI, 47.2865, 11.3989, 48.0, 1200, "Innsbruck"),
        Venue("ski_patscherkofel", "Patscherkofelbahn Igls", VenueCategory.SKI, 47.2272, 11.4132, 45.0, 1000, "Igls"),
        Venue("ski_muttereralm", "Muttereralm Götzens/Mutters", VenueCategory.SKI, 47.2268, 11.3725, 42.5, 900, "Mutters"),
        Venue("ski_axamer_lizum", "Axamer Lizum", VenueCategory.SKI, 47.1958, 11.3025, 54.0, 1500, "Axams"),
        Venue("ski_kuehtai", "Kühtai Bergbahnen", VenueCategory.SKI, 47.2140, 11.0205, 52.0, 1200, "Kühtai"),
        Venue("ski_rangger_koepfl", "Oberperfuss Rangger Köpfl", VenueCategory.SKI, 47.2618, 11.2394, 38.0, 800, "Oberperfuss"),
        Venue("ski_glungezer", "Glungezerbahn Tulfes", VenueCategory.SKI, 47.2642, 11.5325, 43.0, 900, "Tulfes"),
        Venue("ski_seefeld_rosshuette", "Rosshütte Seefeld", VenueCategory.SKI, 47.3326, 11.1965, 56.5, 1100, "Seefeld"),
        Venue("ski_seefeld_gschwandtkopf", "Gschwandtkopf Seefeld", VenueCategory.SKI, 47.3235, 11.1798, 46.0, 900, "Seefeld"),
        Venue("ski_schlick2000", "Schlick 2000 Fulpmes", VenueCategory.SKI, 47.1538, 11.3482, 53.0, 1200, "Stubaital"),
        Venue("ski_stubai_glacier", "Stubaier Gletscher", VenueCategory.SKI, 47.0105, 11.1578, 67.0, 2000, "Stubaital"),
        Venue("ski_serles", "Serlesbahnen Mieders", VenueCategory.SKI, 47.1385, 11.3855, 38.0, 800, "Mieders"),
        Venue("ski_elfer", "Elferbahnen Neustift", VenueCategory.SKI, 47.1082, 11.3072, 39.0, 800, "Neustift"),
        Venue("ski_bergeralm", "Bergeralm Steinach", VenueCategory.SKI, 47.0867, 11.4674, 48.0, 1000, "Steinach am Brenner"),
        Venue("ski_ischgl", "Silvretta Arena Ischgl / Samnaun", VenueCategory.SKI, 47.0118, 10.2912, 76.0, 2000, "Paznaun"),
        Venue("ski_arlberg_stanton", "Ski Arlberg (St. Anton)", VenueCategory.SKI, 47.1296, 10.2685, 78.0, 2500, "St. Anton am Arlberg"),
        Venue("ski_arlberg_lech", "Ski Arlberg (Lech / Zürs / Warth)", VenueCategory.SKI, 47.2104, 10.1425, 78.0, 2500, "Arlberg"),
        Venue("ski_serfaus_fiss_ladis", "Serfaus-Fiss-Ladis", VenueCategory.SKI, 47.0385, 10.6092, 72.0, 2000, "Serfaus"),
        Venue("ski_soelden", "Sölden", VenueCategory.SKI, 46.9678, 11.0075, 74.0, 2000, "Ötztal"),
        Venue("ski_gurgl", "Obergurgl - Hochgurgl", VenueCategory.SKI, 46.8712, 11.0265, 71.0, 1800, "Ötztal"),
        Venue("ski_hochoetz", "Hochoetz", VenueCategory.SKI, 47.2025, 10.9015, 52.0, 1200, "Ötz"),
        Venue("ski_pitztal_gletscher", "Pitztaler Gletscher & Rifflsee", VenueCategory.SKI, 46.9245, 10.8715, 68.0, 1800, "Pitztal"),
        Venue("ski_hochzeiger", "Hochzeiger Jerzens", VenueCategory.SKI, 47.1455, 10.7485, 55.0, 1200, "Pitztal"),
        Venue("ski_kaunertal", "Kaunertaler Gletscher & Fendels", VenueCategory.SKI, 46.9255, 10.7225, 61.0, 1800, "Kaunertal"),
        Venue("ski_venet", "Venet Bergbahnen Landeck/Zams", VenueCategory.SKI, 47.1565, 10.5912, 49.0, 1100, "Landeck"),
        Venue("ski_galtuer", "Silvapark Galtür", VenueCategory.SKI, 46.9685, 10.1875, 57.0, 1200, "Galtür"),
        Venue("ski_kappl", "Bergbahnen Kappl", VenueCategory.SKI, 47.0612, 10.3725, 55.0, 1200, "Kappl"),
        Venue("ski_see", "Bergbahnen See", VenueCategory.SKI, 47.0815, 10.4692, 53.0, 1100, "See"),
        Venue("ski_ehrwalder_alm", "Ehrwalder Alm & Zugspitze", VenueCategory.SKI, 47.3985, 10.9325, 58.5, 1400, "Ehrwald"),
        Venue("ski_lermoos", "Lermoos Grubigstein", VenueCategory.SKI, 47.3995, 10.8812, 56.5, 1200, "Lermoos"),
        Venue("ski_berwang", "Berwang Skischaukel", VenueCategory.SKI, 47.4085, 10.7455, 54.0, 1100, "Berwang"),
        Venue("ski_tannheim", "Tannheimer Tal", VenueCategory.SKI, 47.4985, 10.5185, 49.0, 1200, "Tannheim"),
        Venue("ski_kellerjoch", "Kellerjochbahn Schwaz", VenueCategory.SKI, 47.3411, 11.6668, 39.0, 800, "Schwaz"),
        Venue("ski_spieljoch", "Spieljochbahn Fügen", VenueCategory.SKI, 47.3436, 11.8398, 65.0, 1400, "Zillertal"),
        Venue("ski_hochzillertal", "Hochzillertal Kaltenbach", VenueCategory.SKI, 47.2905, 11.8741, 69.5, 1500, "Kaltenbach"),
        Venue("ski_hochfuegen", "Hochfügen", VenueCategory.SKI, 47.2685, 11.7765, 69.5, 1500, "Hochfügen"),
        Venue("ski_zillertal_arena", "Zillertal Arena (Zell/Gerlos)", VenueCategory.SKI, 47.2341, 11.9025, 72.5, 1800, "Zell am Ziller"),
        Venue("ski_mayrhofen", "Mayrhofner Bergbahnen (Penken)", VenueCategory.SKI, 47.1685, 11.8625, 72.5, 1800, "Mayrhofen"),
        Venue("ski_hintertux", "Hintertuxer Gletscher", VenueCategory.SKI, 47.1085, 11.6755, 72.5, 2000, "Tux"),
        Venue("ski_achensee_rofan", "Rofan Seilbahn Maurach", VenueCategory.SKI, 47.4241, 11.7588, 48.0, 1000, "Maurach"),
        Venue("ski_achensee_karwendel", "Karwendelbahn Pertisau", VenueCategory.SKI, 47.4382, 11.6934, 42.0, 900, "Pertisau"),
        Venue("ski_achensee_christlum", "Hochalmlifte Christlum", VenueCategory.SKI, 47.5305, 11.7012, 49.0, 1100, "Achenkirch"),
        Venue("ski_wildschoenau_markbach", "Markbachjoch Niederau", VenueCategory.SKI, 47.4308, 12.0465, 58.0, 1200, "Wildschönau"),
        Venue("ski_wildschoenau_schatzberg", "Schatzberg Auffach", VenueCategory.SKI, 47.3892, 11.9961, 62.0, 1300, "Wildschönau"),
        Venue("ski_alpbachtal", "Wiedersbergerhorn Alpbach", VenueCategory.SKI, 47.3985, 11.9421, 62.0, 1300, "Alpbach"),
        Venue("ski_skiwelt", "SkiWelt Wilder Kaiser - Brixental", VenueCategory.SKI, 47.4475, 12.1612, 69.0, 2000, "Brixental"),
        Venue("ski_kitzbuehel", "KitzSki Kitzbühel / Kirchberg", VenueCategory.SKI, 47.4412, 12.3895, 72.0, 2200, "Kitzbühel"),
        Venue("ski_st_johann", "Bergbahnen St. Johann in Tirol", VenueCategory.SKI, 47.5192, 12.4345, 59.0, 1300, "St. Johann"),
        Venue("ski_fieberbrunn", "Bergbahnen Fieberbrunn", VenueCategory.SKI, 47.4642, 12.5492, 72.0, 1800, "Pillerseetal"),
        Venue("ski_steinplatte", "Steinplatte Waidring", VenueCategory.SKI, 47.5815, 12.5712, 63.0, 1500, "Waidring"),
        Venue("ski_buchensteinwand", "Buchensteinwand Pillersee", VenueCategory.SKI, 47.5022, 12.5785, 46.0, 900, "St. Ulrich"),
        Venue("ski_koessen", "Unterberg Kössen", VenueCategory.SKI, 47.6612, 12.4085, 48.5, 1100, "Kössen"),
        Venue("ski_grossglockner", "Großglockner Resort Kals-Matrei", VenueCategory.SKI, 47.0012, 12.5412, 59.0, 1500, "Osttirol"),
        Venue("ski_lienz", "Lienzer Bergbahnen (Zettersfeld)", VenueCategory.SKI, 46.8455, 12.7812, 54.0, 1200, "Lienz"),
        Venue("ski_sillian", "Hochpustertal Sillian", VenueCategory.SKI, 46.7455, 12.4185, 56.0, 1200, "Sillian"),

        // ==========================================
        // 2. RODELBAHNEN (SLED)
        // ==========================================
        Venue("rodel_elfer", "Elferbahnen Rodelarena Neustift", VenueCategory.SLED, 47.1082, 11.3072, 34.0, 800, "2x 8 km beschneit & beleuchtet"),
        Venue("rodel_serles", "Serlesbahnen Rodelbahn Mieders", VenueCategory.SLED, 47.1385, 11.3855, 29.0, 800, "Nachtrodeln & Tag"),
        Venue("rodel_bergeralm", "Bergeralm Rodelarena Steinach", VenueCategory.SLED, 47.0867, 11.4674, 32.0, 900, "Beleuchtete Rodelbahn"),
        Venue("rodel_axams", "Axamer Lizum Rodelbahn Birgitzköpfl", VenueCategory.SLED, 47.1958, 11.3025, 24.0, 1000, "Naturrodelbahn"),
        Venue("rodel_rangger", "Rangger Köpfl Naturrodelbahn", VenueCategory.SLED, 47.2618, 11.2394, 26.0, 800, "10 km Rodelabfahrt"),
        Venue("rodel_mutterer", "Muttereralm Rodelbahn", VenueCategory.SLED, 47.2268, 11.3725, 28.0, 900, "Familien- & Sportrodelbahn"),
        Venue("rodel_glungezer", "Glungezer Panoramarodelbahn", VenueCategory.SLED, 47.2642, 11.5325, 30.0, 800, "Panoramabahn bis Mittelstation"),
        Venue("rodel_kellerjoch", "Kellerjoch Rodelbahn Grafenast", VenueCategory.SLED, 47.3411, 11.6668, 25.0, 700, "Längste Rodelbahn Schwaz"),
        Venue("rodel_hochzeiger", "Hochzeiger Rodelbahn (6 km)", VenueCategory.SLED, 47.1455, 10.7485, 31.0, 1000, "Pitztal Rodelspaß"),
        Venue("rodel_nauders", "Bergkastel Rodelbahn Nauders (8 km)", VenueCategory.SLED, 46.8892, 10.5052, 33.0, 1100, "Längste Rodelbahn Tirols"),

        // ==========================================
        // 3. EISLAUFEN & EISSPORT (ICE)
        // ==========================================
        Venue("ice_olympiaworld", "Olympiaworld Innsbruck (Eishalle & Außenring)", VenueCategory.ICE, 47.2575, 11.4101, 7.5, 400, "Große Eislauf-Arena"),
        Venue("ice_sillpark", "Eislaufplatz Sillpark Innsbruck", VenueCategory.ICE, 47.2655, 11.4022, 6.0, 300, "Outdoor Eislaufplatz"),
        Venue("ice_goetzens", "Eissportzentrum Götzens", VenueCategory.ICE, 47.2351, 11.3121, 6.5, 350, "Kunsteisbahn & Publikumslauf"),
        Venue("ice_telfs", "SportZentrum Telfs Eishalle", VenueCategory.ICE, 47.3082, 11.0665, 7.0, 350, "Indoor & Outdoor"),
        Venue("ice_kufstein", "Eisarena Kufstein", VenueCategory.ICE, 47.5812, 12.1645, 7.0, 400, "Kufstein Arena"),
        Venue("ice_wattens", "Kunsteislaufplatz Wattens", VenueCategory.ICE, 47.2882, 11.5912, 5.5, 300, "Freiluft-Kunsteisbahn"),
        Venue("ice_hall", "Eislaufplatz Hall in Tirol", VenueCategory.ICE, 47.2825, 11.5095, 5.5, 300, "Altstadt-Eislaufplatz"),
        Venue("ice_seefeld", "Eissportzentrum Seefeld", VenueCategory.ICE, 47.3292, 11.1852, 7.0, 350, "Kunsteis & Eisstockbahnen"),
        Venue("ice_imst", "Eislaufplatz Imst", VenueCategory.ICE, 47.2415, 10.7392, 6.0, 300, "Gurgltal Kunsteisbahn"),

        // ==========================================
        // 4. BÄDER & BADESEEN (POOL)
        // ==========================================
        Venue("pool_tivoli_hallenbad", "Hallenbad Amraser Straße (Innsbruck)", VenueCategory.POOL, 47.2622, 11.4082, 8.0, 400, "Hallenbad"),
        Venue("pool_olympisches_dorf", "Hallenbad O-Dorf Innsbruck", VenueCategory.POOL, 47.2721, 11.4468, 10.5, 500, "Sauna & Hallenbad"),
        Venue("pool_hoettinger_au", "Hallenbad Höttinger Au Innsbruck", VenueCategory.POOL, 47.2648, 11.3789, 8.0, 400, "Sportbecken"),
        Venue("pool_telfer_bad", "Telfer Bad (Hallenbad & Freibad)", VenueCategory.POOL, 47.3051, 11.0715, 12.5, 500, "Allwetterbad Telfs"),
        Venue("pool_freizeitzentrum_axams", "Freizeitzentrum Axams Hallenbad", VenueCategory.POOL, 47.2285, 11.2785, 10.5, 450, "Hallen- & Freibad"),
        Venue("pool_stuBay", "StuBay Freizeitcenter Telfes", VenueCategory.POOL, 47.1645, 11.3592, 17.5, 600, "Erlebnisbad & Sauna"),
        Venue("pool_wave_woergl", "Wörgler Wasserwelt (Wave)", VenueCategory.POOL, 47.4892, 12.0621, 19.0, 600, "Erlebnisbad"),
        Venue("pool_hall_in_tirol", "Hallenbad Hall in Tirol", VenueCategory.POOL, 47.2831, 11.5122, 8.5, 400, "Hallenbad"),
        Venue("pool_seefeld_olympia", "Olympiabad Seefeld", VenueCategory.POOL, 47.3278, 11.1832, 19.5, 500, "Erlebnis- & Saunawelt"),
        Venue("pool_atoll_achensee", "Atoll Achensee (Maurach)", VenueCategory.POOL, 47.4295, 11.7511, 22.0, 600, "Panorama-Bad am See"),
        Venue("pool_tivoli_freibad", "Tivoli Freibad Innsbruck", VenueCategory.POOL, 47.2585, 11.4112, 7.2, 500, "Freibad"),
        Venue("lake_baggersee_rossau", "Baggersee Rossau Innsbruck", VenueCategory.POOL, 47.2691, 11.4585, 6.5, 600, "Badesee"),
        Venue("lake_natters", "Natterer See", VenueCategory.POOL, 47.2341, 11.3412, 7.0, 500, "Naturbadesee"),
        Venue("lake_lans", "Lanser See", VenueCategory.POOL, 47.2405, 11.4289, 7.0, 400, "Moorbadesee"),
        Venue("pool_hall_freibad", "Freischwimmbad Hall in Tirol", VenueCategory.POOL, 47.2805, 11.5075, 7.0, 450, "Freibad"),
        Venue("pool_wattens_freibad", "Alpenbad Wattens", VenueCategory.POOL, 47.2891, 11.5932, 7.0, 450, "Erlebnisfreibad"),
        Venue("pool_schwaz_freibad", "Erlebnisfreibad Schwaz", VenueCategory.POOL, 47.3465, 11.7051, 6.5, 450, "Freibad"),
        Venue("lake_weissensee_tirol", "Badesee Weißlahn Terfens", VenueCategory.POOL, 47.3235, 11.6421, 6.0, 500, "Freizeitzentrum"),

        // ==========================================
        // 5. KULTUR & SEHENSWÜRDIGKEITEN (CULTURE)
        // ==========================================
        Venue("cult_alpenzoo", "Alpenzoo Innsbruck", VenueCategory.CULTURE, 47.2818, 11.3985, 15.0, 400, "Themenzoo der Alpen"),
        Venue("cult_bergisel", "Bergiselschanze Innsbruck", VenueCategory.CULTURE, 47.2488, 11.3995, 12.0, 350, "Stadion & Turm-Aussicht"),
        Venue("cult_ferdinandeum", "Tiroler Landesmuseum Ferdinandeum", VenueCategory.CULTURE, 47.2675, 11.3972, 13.0, 300, "Kunst- und Kulturgeschichte"),
        Venue("cult_zeughaus", "Museum im Zeughaus Innsbruck", VenueCategory.CULTURE, 47.2701, 11.4085, 8.5, 300, "Maximilianisches Zeughaus"),
        Venue("cult_volkskunstmuseum", "Tiroler Volkskunstmuseum", VenueCategory.CULTURE, 47.2685, 11.3955, 13.0, 300, "Hofkirche & Stuben"),
        Venue("cult_tirol_panorama", "Das Tirol Panorama mit Kaiserjägermuseum", VenueCategory.CULTURE, 47.2505, 11.4012, 13.0, 350, "Riesenrundgemälde"),
        Venue("cult_schloss_ambras", "Schloss Ambras Innsbruck", VenueCategory.CULTURE, 47.2562, 11.4342, 17.0, 500, "Kunst- & Wunderkammer"),
        Venue("cult_muenze_hall", "Burg Hasegg / Münze Hall", VenueCategory.CULTURE, 47.2795, 11.5065, 12.5, 300, "Wiege des Talers"),
        Venue("cult_silberbergwerk", "Schwazer Silberbergwerk", VenueCategory.CULTURE, 47.3512, 11.7225, 20.0, 400, "Mutter aller Bergwerke")
    )

    fun findById(id: String): Venue? = venues.firstOrNull { it.id == id }
}