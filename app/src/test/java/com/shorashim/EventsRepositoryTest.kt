package com.shorashim

import com.shorashim.core.model.CalendarEvent
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.Family
import com.shorashim.core.model.FamilyMember
import com.shorashim.core.model.FamilySummary
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.model.MembershipRole
import com.shorashim.core.model.Person
import com.shorashim.data.events.DefaultEventsRepository
import com.shorashim.data.family.FamilyRepository
import com.shorashim.data.family.PersonRepository
import com.shorashim.data.memorials.InMemoryMemorialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsRepositoryTest {

    private class FakePersonRepository(private val people: List<Person>) : PersonRepository {
        override suspend fun getPeople(familyId: String): Result<List<Person>> = Result.success(people)
        override suspend fun getAllAccessiblePeople(): Result<List<Person>> = Result.success(people)
        override suspend fun getPerson(personId: String): Result<Person> = Result.success(people.first())
        override suspend fun createPerson(person: Person): Result<Person> = Result.success(person)
        override suspend fun updatePerson(person: Person): Result<Person> = Result.success(person)
        override suspend fun deletePerson(personId: String): Result<Unit> = Result.success(Unit)
        override suspend fun searchPeople(familyId: String, query: String): Result<List<Person>> = Result.success(people)
        override suspend fun updatePersonPosition(personId: String, canvasX: Double, canvasY: Double): Result<Unit> = Result.success(Unit)
    }

    private class FakeFamilyRepository(private val families: List<Family>) : FamilyRepository {
        override suspend fun getMyFamilies(): Result<List<Family>> = Result.success(families)
        override suspend fun getFamily(familyId: String): Result<Family> = Result.success(families.first())
        override suspend fun createFamily(name: String, description: String?): Result<Family> = Result.success(families.first())
        override suspend fun renameFamily(familyId: String, newName: String): Result<Family> = Result.success(families.first())
        override suspend fun publishFamily(familyId: String): Result<Family> = Result.success(families.first())
        override suspend fun unpublishFamily(familyId: String): Result<Family> = Result.success(families.first())
        override suspend fun deleteFamily(familyId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getFamilyMembers(familyId: String): Result<List<FamilyMember>> = Result.success(emptyList())
        override suspend fun updateMemberRole(familyId: String, userId: String, role: MembershipRole): Result<Unit> = Result.success(Unit)
        override suspend fun inviteMember(familyId: String, email: String, role: MembershipRole): Result<Unit> = Result.success(Unit)
        override suspend fun removeMember(familyId: String, userId: String): Result<Unit> = Result.success(Unit)
        override suspend fun setFamilyPrivacy(familyId: String, isPrivate: Boolean): Result<Family> = Result.success(families.first())
        override fun observeFamilySummary(): Flow<FamilySummary> = MutableStateFlow(FamilySummary())
    }

    @Test
    fun loadUpcomingEvents_automaticallyDerivesMemorialFromDeceasedPerson() = runTest {
        val testFamily = Family(id = "fam_1", name = "וקנין", ownerId = "u1")
        val deceasedPerson = Person(
            id = "p_deceased",
            familyId = "fam_1",
            firstName = "יעקב",
            lastName = "וקנין",
            lifeStatus = LifeStatus.DECEASED,
            deathDate = "1998-11-20",
            deathDatePrecision = DatePrecision.FULL_DATE,
        )
        val livingPerson = Person(
            id = "p_living",
            familyId = "fam_1",
            firstName = "דוד",
            lastName = "וקנין",
            lifeStatus = LifeStatus.LIVING,
            birthDate = "2000-05-15",
            birthDatePrecision = DatePrecision.FULL_DATE,
        )

        val repo = DefaultEventsRepository(
            personRepository = FakePersonRepository(listOf(deceasedPerson, livingPerson)),
            familyRepository = FakeFamilyRepository(listOf(testFamily)),
            memorialRepository = InMemoryMemorialRepository(),
        )

        val eventsResult = repo.loadUpcomingEvents()
        assertTrue(eventsResult.isSuccess)
        val events = eventsResult.getOrThrow()

        // Should have 1 birthday and 1 memorial
        assertEquals(2, events.size)

        val memorial = events.filterIsInstance<CalendarEvent.Memorial>().firstOrNull()
        assertTrue(memorial != null)
        assertEquals("יעקב וקנין", memorial?.personName)
        assertEquals("וקנין", memorial?.familyName)
        assertTrue(memorial?.description?.contains("אזכרה") == true)
        assertTrue(memorial?.description?.contains("20/11/1998") == true)

        val birthday = events.filterIsInstance<CalendarEvent.Birthday>().firstOrNull()
        assertTrue(birthday != null)
        assertEquals("דוד וקנין", birthday?.personName)
    }

    @Test
    fun loadUpcomingEvents_ignoresDeceasedWithoutDeathDate() = runTest {
        val testFamily = Family(id = "fam_1", name = "וקנין", ownerId = "u1")
        val deceasedNoDate = Person(
            id = "p_deceased_no_date",
            familyId = "fam_1",
            firstName = "משה",
            lastName = "וקנין",
            lifeStatus = LifeStatus.DECEASED,
            deathDate = null,
        )

        val repo = DefaultEventsRepository(
            personRepository = FakePersonRepository(listOf(deceasedNoDate)),
            familyRepository = FakeFamilyRepository(listOf(testFamily)),
            memorialRepository = InMemoryMemorialRepository(),
        )

        val events = repo.loadUpcomingEvents().getOrThrow()
        assertTrue(events.isEmpty())
    }

    @Test
    fun loadUpcomingEvents_deceasedPersonWithBirthDate_doesNotGetBirthday() = runTest {
        val testFamily = Family(id = "fam_1", name = "וקנין", ownerId = "u1")
        val deceasedWithBothDates = Person(
            id = "p_deceased_both",
            familyId = "fam_1",
            firstName = "נאטר",
            lastName = "וקהני",
            lifeStatus = LifeStatus.DECEASED,
            birthDate = "1950-03-10",
            birthDatePrecision = DatePrecision.FULL_DATE,
            deathDate = "2020-08-15",
            deathDatePrecision = DatePrecision.FULL_DATE,
        )

        val repo = DefaultEventsRepository(
            personRepository = FakePersonRepository(listOf(deceasedWithBothDates)),
            familyRepository = FakeFamilyRepository(listOf(testFamily)),
            memorialRepository = InMemoryMemorialRepository(),
        )

        val events = repo.loadUpcomingEvents().getOrThrow()
        val birthdays = events.filterIsInstance<CalendarEvent.Birthday>()
        val memorials = events.filterIsInstance<CalendarEvent.Memorial>()

        // Must NOT have any birthday
        assertTrue("Deceased person must NOT have birthday events", birthdays.isEmpty())
        // Must have memorial
        assertEquals(1, memorials.size)
        assertEquals("נאטר וקהני", memorials.first().personName)
    }
}
