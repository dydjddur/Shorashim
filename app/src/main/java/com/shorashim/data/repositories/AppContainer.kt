package com.shorashim.data.repositories

import android.util.Log
import com.shorashim.BuildConfig
import com.shorashim.data.auth.AuthRepository
import com.shorashim.data.auth.SupabaseAuthRepository
import com.shorashim.data.family.FamilyRepository
import com.shorashim.data.family.PersonRepository
import com.shorashim.data.family.RelationshipRepository
import com.shorashim.data.family.SupabaseFamilyRepository
import com.shorashim.data.family.SupabasePersonRepository
import com.shorashim.data.family.SupabaseRelationshipRepository
import com.shorashim.data.memorials.InMemoryMemorialRepository
import com.shorashim.data.memorials.MemorialRepository
import com.shorashim.data.psalms.PsalmsRepository
import com.shorashim.data.psalms.UnavailablePsalmsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

class AppContainer {
    private val sanitizedUrl: String = BuildConfig.SUPABASE_URL.trim()
        .removeSuffix("/")
        .let { url ->
            when {
                url.endsWith("/rest/v1", ignoreCase = true) -> url.removeSuffix("/rest/v1").removeSuffix("/")
                url.endsWith("/auth/v1", ignoreCase = true) -> url.removeSuffix("/auth/v1").removeSuffix("/")
                url.endsWith("/v1", ignoreCase = true) -> url.removeSuffix("/v1").removeSuffix("/")
                else -> url
            }
        }

    val supabaseClient: SupabaseClient? = if (
        sanitizedUrl.isNotBlank() &&
        BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
    ) {
        try {
            createSupabaseClient(
                supabaseUrl = sanitizedUrl,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim(),
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
                defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(
                    kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                        isLenient = true
                    }
                )
            }
        } catch (e: Throwable) {
            Log.e("AppContainer", "Failed to create SupabaseClient", e)
            null
        }
    } else {
        null
    }

    val authRepository: AuthRepository = SupabaseAuthRepository(supabaseClient)
    val familyRepository: FamilyRepository = SupabaseFamilyRepository(supabaseClient)
    val personRepository: PersonRepository = SupabasePersonRepository(supabaseClient)
    val relationshipRepository: RelationshipRepository = SupabaseRelationshipRepository(supabaseClient)
    val memorialRepository: MemorialRepository = InMemoryMemorialRepository()
    val psalmsRepository: PsalmsRepository = UnavailablePsalmsRepository()
}
