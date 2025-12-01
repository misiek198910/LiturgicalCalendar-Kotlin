package mivs.liturgicalcalendar.data

import mivs.liturgicalcalendar.data.entity.FixedFeastEntity

object FixedFeastsData {
    // Lista "wiecznych" świąt
    val list = listOf(
        // STYCZEŃ
        FixedFeastEntity(1, 1, "Świętej Bożej Rodzicielki Maryi", "w", 3),
        FixedFeastEntity(1, 6, "Objawienie Pańskie (Trzech Króli)", "w", 3),
        FixedFeastEntity(1, 25, "Nawrócenie św. Pawła Apostoła", "w", 2),
        // LUTY
        FixedFeastEntity(2, 2, "Ofiarowanie Pańskie", "w", 2),
        FixedFeastEntity(2, 14, "Św. Cyryla i Metodego", "w", 2),
        // MARZEC
        FixedFeastEntity(3, 19, "Uroczystość św. Józefa, Oblubieńca NMP", "w", 3),
        FixedFeastEntity(3, 25, "Zwiastowanie Pańskie", "w", 3),
        // MAJ
        FixedFeastEntity(5, 3, "NMP Królowej Polski", "w", 3),
        // CZERWIEC
        FixedFeastEntity(6, 24, "Narodzenie św. Jana Chrzciciela", "w", 3),
        FixedFeastEntity(6, 29, "Świętych Apostołów Piotra i Pawła", "r", 3),
        // SIERPIEŃ
        FixedFeastEntity(8, 6, "Przemienienie Pańskie", "w", 2),
        FixedFeastEntity(8, 15, "Wniebowzięcie NMP", "w", 3),
        FixedFeastEntity(8, 26, "NMP Częstochowskiej", "w", 3),
        // WRZESIEŃ
        FixedFeastEntity(9, 8, "Narodzenie NMP", "w", 2),
        FixedFeastEntity(9, 14, "Podwyższenie Krzyża Świętego", "r", 2),
        // LISTOPAD
        FixedFeastEntity(11, 1, "Wszystkich Świętych", "w", 3),
        FixedFeastEntity(11, 2, "Wspomnienie wiernych zmarłych", "v", 1),
        // GRUDZIEŃ
        FixedFeastEntity(12, 8, "Niepokalane Poczęcie NMP", "w", 3),
        FixedFeastEntity(12, 25, "Uroczystość Narodzenia Pańskiego", "w", 3),
        FixedFeastEntity(12, 26, "Św. Szczepana, pierwszego męczennika", "r", 2)
    )
}