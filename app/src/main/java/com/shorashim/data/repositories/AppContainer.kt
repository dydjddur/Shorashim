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
    companion object {
        private const val DEFAULT_SUPABASE_URL = "https://drbhaydqdlupzpyngkdl.supabase.co"
        private const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRyYmhheWRxZGx1cHpweW5na2RsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE2NTAwNTMsImV4cCI6MjEwNzIyNjA1M30.ITyIGFcp5JpLU5sz_TVFQZOG8KoBe8KeQ8ydKZjVBG4"
    }

    private val effectiveUrl: String = BuildConfig.SUPABASE_URL.ifBlank { DEFAULT_SUPABASE_URL }
    private val effectiveKey: String = BuildConfig.SUPABASE_ANON_KEY.ifBlank { DEFAULT_SUPABASE_ANON_KEY }.trim()

    private val sanitizedUrl: String = effectiveUrl.trim()
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
        effectiveKey.isNotBlank()
    ) {
        try {
            createSupabaseClient(
                supabaseUrl = sanitizedUrl,
                supabaseKey = effectiveKey,
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
    val personRepository: PersonRepository = SupabasePersonRepository(
        supabaseClient,
        context ?: com.shorashim.ShorashimApplication.instance,
    )
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
    val friendRepository: com.shorashim.data.friends.FriendRepository = com.shorashim.data.friends.SupabaseFriendRepository(
        supabase = supabaseClient,
        context = context ?: com.shorashim.ShorashimApplication.instance,
    )
    val chatRepository: com.shorashim.data.chat.ChatRepository = com.shorashim.data.chat.SupabaseChatRepository(
        supabase = supabaseClient,
        cryptoManager = com.shorashim.core.crypto.ChatCryptoManager(context ?: com.shorashim.ShorashimApplication.instance),
        context = context ?: com.shorashim.ShorashimApplication.instance,
    )
    val notificationScheduler: EventNotificationScheduler? = context?.let { EventNotificationScheduler(it) }
}
