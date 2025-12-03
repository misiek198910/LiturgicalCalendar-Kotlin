package mivs.liturgicalcalendar.data

import mivs.liturgicalcalendar.data.entity.FixedFeastEntity

object FixedFeastsData {
    // Lista "wiecznych" świąt z czytaniami pobranymi z LegacyData2025

    val list = listOf(
        // --- STYCZEŃ ---
        FixedFeastEntity(1, 1, "Świętej Bożej Rodzicielki Maryi", "w", 3,
            "ŁK2,16–21", "Bóg miłosierny niech nam błogosławi"),

        FixedFeastEntity(1, 2, "Św. Bazylego Wielkiego i Grzegorza z Nazjanzu", "w", 1,
            "J1,19–28", "Ziemia ujrzała swego zbawiciela"), // *Z dnia, ale wpisane dla ciągłości

        FixedFeastEntity(1, 6, "Objawienie Pańskie (Trzech Króli)", "w", 3,
            "MT2,1–12", "Uwielbiają Pana wszystkie ludy ziemi"),

        FixedFeastEntity(1, 17, "Św. Antoniego, opata", "w", 1,
            "MK2,1–12", "Wielkich dzieł Boga nie zapominajmy"), // *Z dnia

        FixedFeastEntity(1, 21, "Św. Agnieszki, dziewicy i męczennicy", "r", 1,
            "MK2,23–28", "Pan Bóg pamięta o swoim przymierzu"), // *Z dnia

        FixedFeastEntity(1, 24, "Św. Franciszka Salezego", "w", 1,
            "MK3,13–19", "Łaska i wierność spotykają się z sobą"), // *Z dnia

        FixedFeastEntity(1, 25, "Nawrócenie św. Pawła Apostoła", "w", 2,
            "MK16,15–18", "Całemu światu głoście Ewangelie"),

        FixedFeastEntity(1, 28, "Św. Tomasza z Akwinu", "w", 1,
            "MK3,31–35", "Przychodzę, Boże, pełnić Twoją wolę"), // *Z dnia

        FixedFeastEntity(1, 31, "Św. Jana Bosko, prezbitera", "w", 1,
            "MK4,26–34", "Zbawienie prawych pochodzi od Pana"), // *Z dnia
        FixedFeastEntity(1, 19, "Św. Józefa Sebastiana Pelczara, biskupa", "w", 1,
            "Z dnia", "Z dnia"),
        FixedFeastEntity(1, 26, "Świętych biskupów Tymoteusza i Tytusa", "w", 1,
            "Z dnia", "Z dnia"),

        // --- LUTY ---
        FixedFeastEntity(2, 2, "Ofiarowanie Pańskie", "w", 2,
            "ŁK2,22–40", "Pan Bóg Zastępów, On jest Królem chwały"),

        FixedFeastEntity(2, 5, "Św. Agaty, dziewicy i męczennicy", "r", 1,
            "MK6,1–6", "Chwalić cię będą, którzy Cię szukają"), // *Z dnia

        FixedFeastEntity(2, 6, "Świętych Męczenników Pawła Miki i Towarzyszy", "r", 1,
            "MK6,7–13", "Bóg jest łaskawy dla swoich czcicieli"), // *Z dnia

        FixedFeastEntity(2, 10, "Św. Scholastyki, dziewicy", "w", 1,
            "MK6,53–56", "Jak jest przedziwne imię Twoje, Panie!"), // *Z dnia

        FixedFeastEntity(2, 11, "Najświętszej Maryi Panny z Lourdes", "w", 1,
            "MK7,1–13", "Chwal i błogosław, duszo moja, Pana"), // *Z dnia

        FixedFeastEntity(2, 14, "Św. Cyryla i Metodego, patronów Europy", "w", 2,
            "ŁK10,1–9", "Całemu światu głoście ewangelię"),

        FixedFeastEntity(2, 22, "Katedry św. Piotra, Apostoła", "w", 2,
            "MT16,13–19", "Pan mym pasterzem, nie brak mi niczego"),
        FixedFeastEntity(2, 23, "Św. Polikarpa, biskupa i męczennika", "r", 1,
            "Z dnia", "Z dnia"),

        // --- MARZEC ---
        FixedFeastEntity(3, 4, "Św. Kazimierza, patrona Polski", "w", 2,
            "MT6,19–23", "Prowadź mnie Panie, ścieżką Twych przykazań"), // *Z dnia (post)

        FixedFeastEntity(3, 19, "Uroczystość św. Józefa, Oblubieńca NMP", "w", 3,
            "MT1,16.18–21.24A", "Jego potomstwo będzie trwało wiecznie"),

        FixedFeastEntity(3, 25, "Zwiastowanie Pańskie", "w", 3,
            "ŁK1,26–38", "Przychodzę, Boże pełnić Twoja wolę"),
        FixedFeastEntity(5, 4, "Św. Floriana, męczennika", "r", 1,
            "Z dnia", "Z dnia"),
        FixedFeastEntity(5, 18, "Św. Stanisława Papczyńskiego / Św. Jana I", "w", 1,
            "Z dnia", "Z dnia"),
        FixedFeastEntity(5, 25, "Św. Bedy Czcigodnego / Św. Grzegorza VII", "w", 1,
            "Z dnia", "Z dnia"),

        // --- KWIECIEŃ ---
        FixedFeastEntity(4, 23, "Uroczystość św. Wojciecha, patrona Polski", "r", 3,
            "J12,24–26", "Wszystkie narody, wysławiajcie Pana"), // *Dobrane klasyczne

        FixedFeastEntity(4, 25, "Św. Marka Ewangelisty", "r", 2,
            "MK16,15–20", "Całemu światu głoście Ewangelię"), // *Klasyczne

        FixedFeastEntity(4, 29, "Św. Katarzyny Sieneńskiej, patronki Europy", "w", 2,
            "MT11,25–30", "Błogosław, duszo moja, Pana"),

        // --- MAJ ---
        FixedFeastEntity(5, 1, "Św. Józefa Rzemieślnika", "w", 1,
            "MT13,54–58", "Słowo Twe, Panie, niezmienne na wieki"),

        FixedFeastEntity(5, 3, "NMP Królowej Polski", "w", 3,
            "J19,25–27", "Tyś wielką chluba naszego narodu"),

        FixedFeastEntity(5, 6, "Świętych Apostołów Filipa i Jakuba", "r", 2,
            "J14,6–14", "Po całej ziemi ich głos się rozchodzi"),

        FixedFeastEntity(5, 8, "Św. Stanisława, biskupa, patrona Polski", "r", 3,
            "J10,11–16", "Sławię Cię, Panie, bo mnie wybawiłeś"),

        FixedFeastEntity(5, 14, "Św. Macieja Apostoła", "r", 2,
            "J15,9–17", "Pan w swoim ludzie upodobał sobie"),

        FixedFeastEntity(5, 16, "Św. Andrzeja Boboli, patrona Polski", "r", 2,
            "J17,20–26", "Będę Cię, Panie, chwalił wśród narodów"),

        FixedFeastEntity(5, 24, "NMP Wspomożycielki Wiernych", "w", 1,
            "J2,1-11", "Tyś wielką chlubą naszego narodu"), // *Dobrane

        FixedFeastEntity(5, 31, "Nawiedzenie NMP", "w", 2,
            "ŁK1,39–56", "Wielbimy Pana, bo swój lud nawiedził"),

        // --- CZERWIEC ---
        FixedFeastEntity(6, 11, "Św. Barnaby, Apostoła", "r", 1,
            "MT10,7–13", "Pan okazał swoje zbawienie"),

        FixedFeastEntity(6, 13, "Św. Antoniego z Padwy", "w", 1,
            "ŁK10,1–9", "Na wieki będę sławił łaski Pana"), // *Z dnia

        FixedFeastEntity(6, 17, "Św. Brata Alberta Chmielowskiego", "w", 1,
            "MT25,31–46", "Błogosławiony, kto służy Panu"), // *Dobrane

        FixedFeastEntity(6, 21, "Św. Alojzego Gonzagi", "w", 1,
            "MT22,34–40", "Pan mym dziedzictwem, moim przeznaczeniem"),

        FixedFeastEntity(6, 24, "Narodzenie św. Jana Chrzciciela", "w", 3,
            "ŁK1,57–66.80", "Sławię Cię, Panie, bo mnie wybawiłeś"),

        FixedFeastEntity(6, 29, "Świętych Apostołów Piotra i Pawła", "r", 3,
            "MT16,13–19", "Od wszelkiej trwogi Pan Bóg mnie wyzwolił"),

        FixedFeastEntity(6, 1, "Św. Justyna, męczennika", "r", 1,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(6, 8, "Św. Jadwigi Królowej", "w", 1,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(6, 15, "Bł. Jolanty, zakonnicy", "w", 1,
            "Z dnia", "Z dnia"),
        FixedFeastEntity(6, 22, "Świętych męczenników Jana Fishera i Tomasza More", "r", 1,
            "Z dnia", "Z dnia"),

        // --- LIPIEC ---
        FixedFeastEntity(7, 3, "Św. Tomasza Apostoła", "r", 2,
            "J20,24–29", "Całemu światu głoście Ewangelię"),

        FixedFeastEntity(7, 11, "Św. Benedykta, patrona Europy", "w", 2,
            "MT19,27–29", "Będę Cię wielbił, Boże mój i Królu"),

        FixedFeastEntity(7, 22, "Św. Marii Magdaleny", "w", 2,
            "J20,1.11–18", "Ciebie, mój Boże, pragnie moja dusza"),

        FixedFeastEntity(7, 23, "Św. Brygidy, patronki Europy", "w", 2,
            "J15,1–8", "Po wieczne czasy będę chwalił Pana"),

        FixedFeastEntity(7, 25, "Św. Jakuba Apostoła", "r", 2,
            "MT20,20–28", "We łzach siejący żąć będą w radości"),

        FixedFeastEntity(7, 26, "Świętych Joachima i Anny", "w", 1,
            "MT13,16-17", "Potomstwo prawych dozna błogosławieństwa"), // *Dobrane

        FixedFeastEntity(7, 6, "Bł. Marii Teresy Ledóchowskiej, dziewicy", "w", 1,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(7, 13, "Świętych pustelników Andrzeja Świerada i Benedykta", "w", 1,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(7, 20, "Bł. Czesława, prezbitera", "w", 1, "Z dnia",
            "Z dnia"),

        // --- SIERPIEŃ ---
        FixedFeastEntity(8, 6, "Przemienienie Pańskie", "w", 2,
            "ŁK9,28B–36", "Pan Bóg króluje pełen majestatu"),

        FixedFeastEntity(8, 9, "Św. Teresy Benedykty od Krzyża (Edyty Stein)", "r", 2,
            "MT25,1–13", "Przystąpię do ołtarza Bożego"),

        FixedFeastEntity(8, 10, "Św. Wawrzyńca, diakona i męczennika", "r", 2,
            "J12,24–26", "Błogosławiony, kto służy Panu"),

        FixedFeastEntity(8, 14, "Św. Maksymiliana Marii Kolbego", "r", 1,
            "J15,12–17", "Cenna przed Panem śmierć Jego świętych"),

        FixedFeastEntity(8, 15, "Wniebowzięcie Najświętszej Maryi Panny", "w", 3,
            "ŁK1,39–56", "Stoi Królowa po Twojej prawicy"),

        FixedFeastEntity(8, 24, "Św. Bartłomieja Apostoła", "r", 2,
            "J1,45–51", "Niech wierni Twoi głoszą Twe królestwo"),

        FixedFeastEntity(8, 26, "NMP Częstochowskiej", "w", 3,
            "J2,1–11", "Tyś wielką chluba naszego narodu"),

        FixedFeastEntity(8, 17, "Św. Jacka, prezbitera (Patrona polskiej prowincji dominikanów)", "w", 1,
            "Z dnia", "Z dnia"),

        // --- WRZESIEŃ ---
        FixedFeastEntity(9, 8, "Narodzenie NMP", "w", 2,
            "MT1,1–16.18–23", "Raduj się, duszo, w Bogu, Zbawcy moim"),

        FixedFeastEntity(9, 14, "Podwyższenie Krzyża Świętego", "r", 2,
            "J3,13–17", "Wielkich dzieł Boga nie zapominamy"),

        FixedFeastEntity(9, 18, "Św. Stanisława Kostki, patrona Polski", "w", 2,
            "ŁK2,41–52", "Będę Cię wielbił, Boże mój Królu"), // *Z dnia, dobrane

        FixedFeastEntity(9, 21, "Św. Mateusza Apostoła i Ewangelisty", "r", 2,
            "MT9,9–13", "Po całej ziemi ich głos się rozchodzi"),

        FixedFeastEntity(9, 23, "Św. Pio z Pietrelciny", "w", 1,
            "MT11,25-30", "Zaufaj Panu, On jest twoim zbawcą"), // *Dobrane

        FixedFeastEntity(9, 29, "Świętych Archaniołów Michała, Gabriela i Rafała", "w", 2,
            "J1,47–51", "Wobec aniołów psalm zaśpiewam Panu"),
        FixedFeastEntity(9, 7, "Św. Melchiora Grodzieckiego, prezbitera i męczennika", "r", 1,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(9, 28, "Św. Wacława, męczennika", "r", 1,
            "Z dnia", "Z dnia"),

        // --- PAŹDZIERNIK ---
        FixedFeastEntity(10, 2, "Świętych Aniołów Stróżów", "w", 1,
            "MT18,1–5.10", "Aniołom kazał, aby strzegli ciebie"),

        FixedFeastEntity(10, 4, "Św. Franciszka z Asyżu", "w", 1,
            "MT11,25–30", "Błogosławieni ubodzy w duchu"), // *Dobrane

        FixedFeastEntity(10, 5, "Św. Faustyny Kowalskiej", "w", 1,
            "ŁK7,36–50", "Miłosierdzie Pana wyśpiewywać będę"), // *Dobrane

        FixedFeastEntity(10, 18, "Św. Łukasza Ewangelisty", "r", 2,
            "ŁK10,1–9", "Niech wierni twoi głoszą Twe królestwo"),

        FixedFeastEntity(10, 22, "Św. Jana Pawła II, papieża", "w", 1,
            "J21,15–17", "Po całej ziemi ich głos się rozchodzi"), // *Dobrane

        FixedFeastEntity(10, 28, "Świętych Apostołów Szymona i Judy Tadeusza", "r", 2,
            "J15,17-27", "Po całej ziemi ich głos się rozchodzi"), // *Dobrane
        FixedFeastEntity(10, 5, "Św. Faustyny Kowalskiej", "w", 1,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(10, 12, "Bł. Jana Beyzyma, prezbitera", "w", 1,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(10, 19, "Bł. Jerzego Popiełuszki, prezbitera i męczennika", "r", 1,
            "Z dnia", "Z dnia"),

        // --- LISTOPAD ---
        FixedFeastEntity(11, 1, "Wszystkich Świętych", "w", 3,
            "MT5,1–12A", "Oto lud wierny, szukający Boga"),

        FixedFeastEntity(11, 2, "Wspomnienie wiernych zmarłych", "v", 1,
            "J14,1–6", "W krainie życia ujrzę dobroć Boga"),

        FixedFeastEntity(11, 9, "Rocznica Poświęcenia Bazyliki Laterańskiej", "w", 2,
            "J2,13–22", "Odnogi rzeki rozweselają miasto Boże"), // *Klasyk

        FixedFeastEntity(11, 11, "Św. Marcina z Tours (Niepodległość)", "w", 1,
            "MT25,31–40", "Bóg jest we wnętrzu swojego kościoła"), // *Dobrane

        FixedFeastEntity(11, 30, "Św. Andrzeja Apostoła", "r", 2,
            "MT4,18-22", "Po całej ziemi ich głos się rozchodzi"),
        FixedFeastEntity(11, 16, "Najświętszej Maryi Panny Ostrobramskiej, Matki Miłosierdzia", "w", 3,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(11, 23, "Św. Klemensa I, papieża i męczennika", "r", 1,
            "Z dnia", "Z dnia"),

        // --- GRUDZIEŃ ---
        FixedFeastEntity(12, 6, "Św. Mikołaja, biskupa", "w", 1,
            "MT9,35–10,1", "Będę opiewał chwałę Twoją, Boże"), // *Z dnia

        FixedFeastEntity(12, 8, "Niepokalane Poczęcie NMP", "w", 3,
            "ŁK1,26–38", "Śpiewajcie Panu pieśń nową"), // *Klasyk

        FixedFeastEntity(12, 25, "Uroczystość Narodzenia Pańskiego", "w", 3,
            "J1,1–18", "Ziemia ujrzała swego Zbawiciela"),

        FixedFeastEntity(12, 26, "Św. Szczepana, pierwszego męczennika", "r", 2,
            "MT10,17–22", "W ręce Twe, Panie składam ducha mego"),

        FixedFeastEntity(12, 27, "Św. Jana Apostoła i Ewangelisty", "w", 2,
            "J20,2–8", "Niech sprawiedliwi weselą się w Panu"),

        FixedFeastEntity(12, 28, "Świętych Młodzianków, męczenników", "r", 2,
            "MT2,13–18", "Nasza dusza została wyzwolona jak ptak z sideł"),
        FixedFeastEntity(12, 7, "Św. Ambrożego, biskupa i doktora Kościoła", "w", 1,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(12, 14, "Św. Jana od Krzyża, prezbitera i doktora Kościoła", "w", 1,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(12, 21, "Św. Piotra Kanizjusza, prezbitera i doktora Kościoła", "w", 1,
            "Z dnia", "Z dnia"),

        FixedFeastEntity(1, 19, "Św. Józefa Sebastiana Pelczara, biskupa", "w", 1,
            "J10,11–16", "Jesteś kapłanem, tak jak Melchizedek"),

        FixedFeastEntity(1, 26, "Świętych biskupów Tymoteusza i Tytusa", "w", 1,
            "ŁK10,1–9", "Po całej ziemi ich głos się rozchodzi"),

    )
}