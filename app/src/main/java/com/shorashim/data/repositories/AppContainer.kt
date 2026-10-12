package com.shorashim.data.repositories

import android.content.Context
import android.util.Log
import com.shorashim.BuildConfig
import com.shorashim.core.notifications.EventNotificationScheduler
import com.shorashim.data.auth.AuthRepository
import com.shorashim.data.auth.SupabaseAuthRepository
import com.shorashim.data.family.FamilyRepository
import com.shorashim.data.family.PersonRepository
import com.shorashim.data.family.RelationshipRepository
import com.shorashim.data.family.SupabaseFamilyRepository
import com.shorashim.data.family.SupabasePersonRepository
import com.shorashim.data.family.SupabaseRelationshipRepository
import com.shorashim.data.invitations.InvitationRepository
import com.shorashim.data.invitations.SupabaseInvitationRepository
import com.shorashim.data.memorials.MemorialRepository
import com.shorashim.data.memorials.SupabaseMemorialRepository
import com.shorashim.data.profile.ProfileRepository
import com.shorashim.data.profile.SupabaseProfileRepository
import com.shorashim.data.psalms.LocalPsalmsRepository
import com.shorashim.data.psalms.PsalmsRepository
import com.shorashim.data.psalms.UnavailablePsalmsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

class AppContainer(
    val context: Context? = null,
) {
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
                install(io.github.jan.supabase.realtime.Realtime)
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
    val memorialRepository: MemorialRepository = SupabaseMemorialRepository(supabaseClient)
    val eventsRepository: com.shorashim.data.events.EventsRepository = com.shorashim.data.events.DefaultEventsRepository(
        personRepository = personRepository,
        familyRepository = familyRepository,
        memorialRepository = memorialRepository,
    )
    val psalmsRepository: PsalmsRepository = if (context != null) LocalPsalmsRepository(context) else UnavailablePsalmsRepository()
    val profileRepository: ProfileRepository = SupabaseProfileRepository(supabaseClient)
    val invitationRepository: InvitationRepository = SupabaseInvitationRepository(supabaseClient)
    val notificationScheduler: EventNotificationScheduler? = context?.let { EventNotificationScheduler(it) }
}
