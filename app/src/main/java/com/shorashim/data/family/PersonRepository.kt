package com.shorashim.data.family

import com.shorashim.core.model.Person

interface PersonRepository {
    suspend fun getPeople(familyId: String): Result<List<Person>>
    suspend fun getAllAccessiblePeople(): Result<List<Person>>
    suspend fun getPerson(personId: String): Result<Person>
    suspend fun createPerson(person: Person): Result<Person>
    suspend fun updatePerson(person: Person): Result<Person>
    suspend fun deletePerson(personId: String): Result<Unit>
    suspend fun searchPeople(familyId: String, query: String): Result<List<Person>>
    suspend fun updatePersonPosition(personId: String, canvasX: Double, canvasY: Double): Result<Unit>
}
