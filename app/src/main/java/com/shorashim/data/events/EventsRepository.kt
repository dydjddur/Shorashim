package com.shorashim.data.events

import android.util.Log
import com.shorashim.core.model.CalendarEvent
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.EventDateCalculator
import com.shorashim.core.model.MemorialEvent
import com.shorashim.core.model.MemorialEventInsertDto
import com.shorashim.data.family.FamilyRepository
import com.shorashim.data.family.PersonRepository
import com.shorashim.data.memorials.MemorialRepository
import java.time.LocalDate

interface EventsRepository {
    suspend fun loadUpcomingEvents(): Result<List<CalendarEvent>>
    suspend fun createMemorialEvent(insertDto: MemorialEventInsertDto): Result<MemorialEvent>
    suspend fun updateMemorialEvent(id: String, insertDto: MemorialEventInsertDto): Result<MemorialEvent>
    suspend fun deleteMemorialEvent(id: String): Result<Unit>
}

class DefaultEventsRepository(
    private val personRepository: PersonRepository,
    private val familyRepository: FamilyRepository,
    private val memorialRepository: MemorialRepository,
) : EventsRepository {

    override suspend fun loadUpcomingEvents(): Result<List<CalendarEvent>> {
        return runCatching {
            val today = EventDateCalculator.getTodayInIsrael()

            // 1. Fetch all accessible families to map names
            val familiesResult = familyRepository.getMyFamilies()
            val familyMap = familiesResult.getOrNull()?.associate { it.id to it.name } ?: emptyMap()

            // 2. Fetch all accessible people across all trees
            val peopleResult = personRepository.getAllAccessiblePeople()
            val people = peopleResult.getOrElse { emptyList() }

            // 3. Generate birthday events with deduplication
            // Deduplicate by normalized (firstName, lastName, birthDate) to avoid duplicate cards if same person is in multiple trees
            val birthdayEvents = mutableListOf<CalendarEvent.Birthday>()
            val processedPeople = mutableSetOf<String>()

            // Group people by identity key: "$firstName|$lastName|$birthDate"
            val groupedPeople = people.groupBy { person ->
                "${person.firstName.trim().lowercase()}|${person.lastName?.trim()?.lowercase().orEmpty()}|${person.birthDate.orEmpty()}"
            }

            for ((key, duplicates) in groupedPeople) {
                val primaryPerson = duplicates.first()
                if (primaryPerson.birthDate.isNullOrBlank()) continue
                // Deceased relatives must ONLY have memorial events, never birthdays
                if (duplicates.any { it.lifeStatus == com.shorashim.core.model.LifeStatus.DECEASED }) {
                    continue
                }
                // If precision is known to be YEAR_ONLY or UNKNOWN, skip because month/day are unknown
                if (primaryPerson.birthDatePrecision == DatePrecision.YEAR_ONLY ||
                    primaryPerson.birthDatePrecision == DatePrecision.UNKNOWN
                ) {
                    continue
                }

                val nextBday = EventDateCalculator.calculateNextBirthday(primaryPerson.birthDate, today) ?: continue
                val daysUntil = EventDateCalculator.calculateDaysUntil(nextBday, today)
                val relativeLabel = EventDateCalculator.formatRelativeLabel(nextBday, today)
                val isToday = daysUntil == 0L
                val message = EventDateCalculator.getBirthdayMessage(primaryPerson.fullName, isToday)

                val treeNames = duplicates.mapNotNull { familyMap[it.familyId] }.distinct()

                birthdayEvents.add(
                    CalendarEvent.Birthday(
                        id = "bday_${primaryPerson.id}",
                        personId = primaryPerson.id,
                        personName = primaryPerson.fullName,
                        familyNames = treeNames,
                        birthDateHistorical = primaryPerson.birthDate,
                        targetDate = nextBday,
                        daysUntil = daysUntil,
                        relativeLabel = relativeLabel,
                        message = message,
                    )
                )
            }

            // 4. Derive memorial events automatically from deceased relatives' deathDate
            val deceasedPeople = people.filter { person ->
                person.lifeStatus == com.shorashim.core.model.LifeStatus.DECEASED &&
                    !person.deathDate.isNullOrBlank() &&
                    person.deathDatePrecision != DatePrecision.YEAR_ONLY &&
                    person.deathDatePrecision != DatePrecision.UNKNOWN
            }

            val groupedDeceased = deceasedPeople.groupBy { person ->
                "${person.firstName.trim().lowercase()}|${person.lastName?.trim()?.lowercase().orEmpty()}|${person.deathDate.orEmpty()}"
            }

            val memorialCalendarEvents = mutableListOf<CalendarEvent.Memorial>()

            for ((_, duplicates) in groupedDeceased) {
                val primaryPerson = duplicates.first()
                val deathDateStr = primaryPerson.deathDate ?: continue
                val nextMemorial = EventDateCalculator.calculateNextMemorialDate(deathDateStr, today) ?: continue
                val daysUntil = EventDateCalculator.calculateDaysUntil(nextMemorial, today)
                val relativeLabel = EventDateCalculator.formatRelativeLabel(nextMemorial, today)
                val treeNames = duplicates.mapNotNull { familyMap[it.familyId] }.distinct()
                val familyName = treeNames.joinToString(", ").ifBlank { "עץ משפחה" }

                val deathYear = deathDateStr.split("-").firstOrNull()?.toIntOrNull()
                val yearsSincePassing = if (deathYear != null) nextMemorial.year - deathYear else null
                val formattedOriginalDate = formatIsoDateToDisplay(deathDateStr)

                val description = if (yearsSincePassing != null && yearsSincePassing > 0) {
                    "אזכרה - שנה $yearsSincePassing לפטירה (נפטר/ה ב-$formattedOriginalDate)"
                } else {
                    "אזכרה שנתית (נפטר/ה ב-$formattedOriginalDate)"
                }

                memorialCalendarEvents.add(
                    CalendarEvent.Memorial(
                        id = "mem_person_${primaryPerson.id}",
                        memorialId = primaryPerson.id,
                        personId = primaryPerson.id,
                        personName = primaryPerson.fullName,
                        familyId = primaryPerson.familyId,
                        familyName = familyName,
                        originalDateStr = formattedOriginalDate,
                        targetDate = nextMemorial,
                        hebrewDate = null,
                        description = description,
                        daysUntil = daysUntil,
                        relativeLabel = relativeLabel,
                        canEdit = false,
                    )
                )
            }

            // Also merge any standalone manual memorial events from DB
            val memorialsResult = memorialRepository.getMemorialEvents()
            val existingPersonIds = memorialCalendarEvents.mapNotNull { it.personId }.toSet()
            val manualMemorials = memorialsResult.getOrElse { emptyList() }
                .filter { it.personId == null || it.personId !in existingPersonIds }

            for (mem in manualMemorials) {
                val nextDate = EventDateCalculator.calculateNextMemorialDate(mem.eventDate, today) ?: continue
                val daysUntil = EventDateCalculator.calculateDaysUntil(nextDate, today)
                val relativeLabel = EventDateCalculator.formatRelativeLabel(nextDate, today)
                val familyName = familyMap[mem.familyId] ?: mem.familyName.ifBlank { "משפחה" }

                memorialCalendarEvents.add(
                    CalendarEvent.Memorial(
                        id = "mem_${mem.id}",
                        memorialId = mem.id,
                        personId = mem.personId,
                        personName = mem.personName,
                        familyId = mem.familyId,
                        familyName = familyName,
                        originalDateStr = formatIsoDateToDisplay(mem.eventDate),
                        targetDate = nextDate,
                        hebrewDate = mem.hebrewDate,
                        description = mem.description,
                        daysUntil = daysUntil,
                        relativeLabel = relativeLabel,
                        canEdit = true,
                    )
                )
            }

            // 5. Combine and sort chronologically, nearest upcoming first
            val combined = (birthdayEvents + memorialCalendarEvents).sortedWith(
                compareBy<CalendarEvent> { it.daysUntil }.thenBy { it.personName }
            )

            combined
        }.onFailure { e ->
            Log.e("EventsRepository", "Failed to load upcoming events", e)
        }
    }

    override suspend fun createMemorialEvent(insertDto: MemorialEventInsertDto): Result<MemorialEvent> {
        return memorialRepository.createMemorialEvent(insertDto)
    }

    override suspend fun updateMemorialEvent(id: String, insertDto: MemorialEventInsertDto): Result<MemorialEvent> {
        return memorialRepository.updateMemorialEvent(id, insertDto)
    }

    override suspend fun deleteMemorialEvent(id: String): Result<Unit> {
        return memorialRepository.deleteMemorialEvent(id)
    }
}

private fun formatIsoDateToDisplay(isoDate: String): String {
    val parts = isoDate.trim().split("-")
    return if (parts.size == 3) {
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } else {
        isoDate
    }
}

