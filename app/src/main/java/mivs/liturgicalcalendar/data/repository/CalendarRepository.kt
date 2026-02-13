package mivs.liturgicalcalendar.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mivs.liturgicalcalendar.data.LectionaryMap
import mivs.liturgicalcalendar.data.db.AppDatabase
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.domain.model.LiturgicalSeason
import java.time.LocalDate

class CalendarRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val fixedDao = db.fixedFeastDao()
    private val movableDao = db.movableFeastDao()
    private val gospelDao = db.gospelDao()
    private val psalmDao = db.psalmDao()

    data class DayReadings(
        val gospelSigla: String,
        val psalmResponse: String,
        val psalmSigla: String? = null,
        val psalmFullText: String? = null,
        val dbFeastName: String? = null,
        val gospelFullText: String? = null
    )

    
    private suspend fun findSmartGospelText(sigla: String): String? {
        val variants = mutableListOf<String>()
        variants.add(sigla)
        variants.add(sigla.replace(" ", ""))
        variants.add(sigla.replace(" ", "").replace("–", "-").replace("—", "-"))
        variants.add(sigla.replace("–", "-"))
        variants.add(sigla.replace("-", "–"))

        for (variant in variants) {
            var text = gospelDao.getGospel(variant)
            if (text != null) return text
            text = gospelDao.getGospel(variant.uppercase())
            if (text != null) return text
        }
        return null
    }

    private suspend fun findSmartPsalmText(sigla: String): String? {
        val variants = mutableListOf<String>()
        variants.add(sigla)

        val normalizedDash = sigla.replace("–", "-").replace("—", "-")
        variants.add(normalizedDash)

        if (normalizedDash.startsWith("Ps ")) {
            variants.add(normalizedDash.replaceFirst("Ps ", "Ps"))
        }

        val noWhitespace = normalizedDash.replace("\\s".toRegex(), "")
        variants.add(noWhitespace)

        variants.add(noWhitespace.replace(".", "").replace(",", ""))

        for (variant in variants) {
            val text = psalmDao.getPsalm(variant)
            if (text != null) return text
        }
        return null
    }

    
    suspend fun getReadingsForDay(dayInfo: LiturgicalDay): DayReadings = withContext(Dispatchers.IO) {
        
        
        
        

        var sigla = "Patrz lekcjonarz"
        var psalmDisplay: String? = "Brak danych"
        var psalmLookupSigla: String? = null
        var psalmFullContent: String? = null
        var feastName: String? = null
        var isPsalmFoundInFixed = false

        
        val isFeriaKey = dayInfo.feastKey?.contains("_W") == true || dayInfo.feastKey?.startsWith("CHRISTMAS_JAN_") == true

        if (dayInfo.feastKey != null && !isFeriaKey && dayInfo.feastKey != "EPIPHANY" && dayInfo.feastKey != "CHRISTMAS_EVE") {
            val movable = movableDao.getFeast(dayInfo.feastKey)
            if (movable != null) {
                
                sigla = movable.gospelSigla
                

                
                val mPsalmSigla = movable.psalmSigla
                if (!mPsalmSigla.isNullOrBlank() && mPsalmSigla != "Z dnia") {
                    psalmLookupSigla = mPsalmSigla
                    val fullText = findSmartPsalmText(mPsalmSigla)

                    if (!fullText.isNullOrEmpty()) {
                        psalmFullContent = fullText
                        psalmDisplay = "Psalm: $mPsalmSigla, Ref: ${movable.psalmResponse}"
                        isPsalmFoundInFixed = true
                        
                    } else {
                        psalmDisplay = "Psalm: $mPsalmSigla (BRAK W BAZIE)"
                        psalmFullContent = "⚠️ BRAK PSALMU: $mPsalmSigla"
                        
                        
                        isPsalmFoundInFixed = true
                        
                    }
                } else {
                    
                    psalmDisplay = "Psalm: ${movable.psalmResponse}"
                    psalmFullContent = "Refren: ${movable.psalmResponse}"
                    isPsalmFoundInFixed = true
                    
                }
            } else {
                
            }
        }

        
        val fixedFeast = fixedDao.getFeast(dayInfo.date.monthValue, dayInfo.date.dayOfMonth)

        if (fixedFeast != null && dayInfo.feastName == fixedFeast.feastName) {
            

            if (fixedFeast.gospelSigla != "Z dnia") {
                sigla = fixedFeast.gospelSigla
                
            }

            val specificPsalmSigla = fixedFeast.psalmSigla
            if (specificPsalmSigla != null && specificPsalmSigla != "Z dnia" && specificPsalmSigla.isNotBlank()) {
                val fullText = findSmartPsalmText(specificPsalmSigla)
                if (!fullText.isNullOrEmpty()) {
                    psalmFullContent = fullText
                    psalmDisplay = "Psalm: $specificPsalmSigla, Ref: ${fixedFeast.psalmResponse}"
                    psalmLookupSigla = specificPsalmSigla
                    isPsalmFoundInFixed = true
                    
                } else {
                    psalmDisplay = "Psalm: $specificPsalmSigla (BRAK W BAZIE)"
                    psalmFullContent = "⚠️ BRAK PSALMU: $specificPsalmSigla"
                    isPsalmFoundInFixed = true
                    
                }
            } else if (fixedFeast.psalmResponse != "Z dnia") {
                psalmDisplay = "Psalm: ${fixedFeast.psalmResponse}"
                psalmFullContent = "Refren: ${fixedFeast.psalmResponse}"
            }
            feastName = fixedFeast.feastName
        }

        
        
        
        if (dayInfo.lectionaryKey != null) {

            
            if (sigla == "Patrz lekcjonarz" || sigla == "Z dnia") {
                val mappedGospel = LectionaryMap.getSigla(dayInfo.lectionaryKey)
                
                if (mappedGospel != null) sigla = mappedGospel
            }

            
            if (!isPsalmFoundInFixed) {
                val mappedPsalmData = LectionaryMap.getPsalmData(dayInfo.lectionaryKey)
                if (mappedPsalmData != null) {
                    
                    val (rawSigla, rawRefrain) = mappedPsalmData
                    

                    val finalRefrain = if (psalmDisplay != "Brak danych" && psalmDisplay?.contains("Ref") == true) {
                        psalmDisplay!!.substringAfter("Ref: ").trim()
                    } else rawRefrain

                    psalmLookupSigla = rawSigla
                    val fullContentFromDb = findSmartPsalmText(rawSigla)

                    if (!fullContentFromDb.isNullOrEmpty()) {
                        psalmFullContent = fullContentFromDb
                        psalmDisplay = "Psalm: $rawSigla, Ref: $finalRefrain"
                        
                    } else {
                        psalmDisplay = "Psalm: $rawSigla (BRAK W BAZIE)"
                        psalmFullContent = "⚠️ BRAK PSALMU: $rawSigla"
                        
                    }
                }
            }
        }

        
        var gospelFullText = findSmartGospelText(sigla)

        if (gospelFullText == null && sigla != "Patrz lekcjonarz") {
            gospelFullText = "⚠️ BRAK EWANGELII: $sigla"
            
        } else if (gospelFullText != null) {
            
        }

        

        return@withContext DayReadings(
            gospelSigla = sigla,
            psalmResponse = psalmDisplay ?: "Brak danych",
            psalmSigla = psalmLookupSigla,
            psalmFullText = psalmFullContent,
            dbFeastName = feastName,
            gospelFullText = gospelFullText
        )
    }

    
    private fun shouldOverwrite(day: LiturgicalDay, fixedRank: Int): Boolean {

        
        if (day.feastKey != null) {
            val isOverwritableKey = day.feastKey == "EPIPHANY"
            
            if (!isOverwritableKey) return false
        }

        

        
        
        if (day.season == LiturgicalSeason.TRIDUUM) return false
        if (day.season == LiturgicalSeason.LENT && day.lectionaryKey?.contains("LENT_W6") == true) return false
        if (day.season == LiturgicalSeason.LENT && day.lectionaryKey?.contains("HOLY_WEEK") == true) return false

        
        
        if (day.season == LiturgicalSeason.EASTER && day.lectionaryKey?.contains("EASTER_W1") == true) return false

        
        
        if (day.season == LiturgicalSeason.LENT) {
            return fixedRank >= 3
        }

        
        if (day.lectionaryKey?.contains("SUN") == true) {
            if (day.lectionaryKey.contains("ADVENT") ||
                day.lectionaryKey.contains("LENT") ||
                day.lectionaryKey.contains("EASTER")) {
                return false
            }
        }

        
        if (day.season == LiturgicalSeason.CHRISTMAS) {
            return true
        }

        
        return fixedRank >= 1
    }

    
    suspend fun getDay(date: LocalDate): LiturgicalDay = withContext(Dispatchers.IO) {
        var day = LiturgicalCalendarCalc.generateDay(date)
        if (day.feastKey == "CHRIST_KING") day = day.copy(colorCode = "w")

        val fixedFeast = fixedDao.getFeast(date.monthValue, date.dayOfMonth)
        if (fixedFeast != null && shouldOverwrite(day, fixedFeast.rank)) {
            val keyForFeast = if (fixedFeast.psalmSigla == "Z dnia" || fixedFeast.gospelSigla == "Z dnia") {
                day.lectionaryKey
            } else {
                fixedFeast.gospelSigla
            }

            day = day.copy(
                feastName = fixedFeast.feastName,
                lectionaryKey = keyForFeast,
                colorCode = fixedFeast.color,
                feastKey = fixedFeast.gospelSigla
            )
        }
        return@withContext day
    }

    
    suspend fun getDaysForMonth(year: Int, month: Int): List<LiturgicalDay> = withContext(Dispatchers.IO) {
        val days = mutableListOf<LiturgicalDay>()
        val start = LocalDate.of(year, month, 1)
        val len = start.lengthOfMonth()

        for (i in 0 until len) {
            val date = start.plusDays(i.toLong())
            var day = LiturgicalCalendarCalc.generateDay(date)

            if (day.feastKey == "CHRIST_KING") day = day.copy(colorCode = "w")

            val fixedFeast = fixedDao.getFeast(date.monthValue, date.dayOfMonth)
            if (fixedFeast != null && shouldOverwrite(day, fixedFeast.rank)) {
                val keyForFeast = if (fixedFeast.psalmSigla == "Z dnia" || fixedFeast.gospelSigla == "Z dnia") {
                    day.lectionaryKey
                } else {
                    fixedFeast.gospelSigla
                }

                day = day.copy(
                    feastName = fixedFeast.feastName,
                    lectionaryKey = keyForFeast,
                    colorCode = fixedFeast.color,
                    feastKey = fixedFeast.gospelSigla
                )
            }
            days.add(day)
        }
        days
    }
}