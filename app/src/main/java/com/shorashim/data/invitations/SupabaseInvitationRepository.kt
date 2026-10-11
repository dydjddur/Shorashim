package com.shorashim.data.invitations

import android.util.Log
import com.shorashim.core.model.FamilyInvitation
import com.shorashim.core.model.FamilyInvitationDto
import com.shorashim.core.model.InvitationResolution
import com.shorashim.core.model.InvitationStatus
import com.shorashim.core.model.MembershipRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

private const val TAG = "SupabaseInvitationRepo"
private const val TABLE = "family_invitations"

class SupabaseInvitationRepository(
    private val supabaseClient: SupabaseClient?,
) : InvitationRepository {

    private val client get() = supabaseClient
        ?: error("Supabase not configured")

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------

    override suspend fun createInvitation(
        familyId: String,
        expiresInDays: Int,
        role: MembershipRole,
    ): Result<FamilyInvitation> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("User not authenticated")

        val insertData = mapOf(
            "family_id"  to familyId,
            "created_by" to userId,
            "role"       to role.name,
            "expires_at" to "NOW() + INTERVAL '$expiresInDays days'",
        )

        // Use RPC-style insert that returns the created row
        val result = client.postgrest[TABLE]
            .insert(buildInsertDto(familyId, userId, role, expiresInDays)) {
                select(Columns.ALL)
            }
            .decodeSingle<FamilyInvitationDto>()

        result.toDomain()
    }.also { if (it.isFailure) Log.e(TAG, "createInvitation failed", it.exceptionOrNull()) }

    private fun buildInsertDto(
        familyId: String,
        userId: String,
        role: MembershipRole,
        expiresInDays: Int,
    ) = FamilyInvitationInsert(
        familyId = familyId,
        createdBy = userId,
        role = role,
        expiresInDays = expiresInDays,
    )

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    override suspend fun getMyPendingInvitations(): Result<List<FamilyInvitation>> =
        runCatching {
            val user = client.auth.currentUserOrNull()
                ?: error("User not authenticated")

            // RLS will filter to rows where invitee_user_id = auth.uid()
            // OR invitee_email = user's email
            // We fetch both and union them client-side via RLS
            client.postgrest[TABLE]
                .select {
                    filter {
                        eq("status", "PENDING")
                    }
                }
                .decodeList<FamilyInvitationDto>()
                .map { it.toDomain() }
        }.also { if (it.isFailure) Log.e(TAG, "getMyPendingInvitations failed", it.exceptionOrNull()) }

    override suspend fun getFamilyInvitations(familyId: String): Result<List<FamilyInvitation>> =
        runCatching {
            client.postgrest[TABLE]
                .select {
                    filter { eq("family_id", familyId) }
                }
                .decodeList<FamilyInvitationDto>()
                .map { it.toDomain() }
        }.also { if (it.isFailure) Log.e(TAG, "getFamilyInvitations failed", it.exceptionOrNull()) }

    // -----------------------------------------------------------------------
    // Accept / Decline / Revoke
    // -----------------------------------------------------------------------

    override suspend fun acceptInvitationByToken(token: String): Result<InvitationResolution> =
        runCatching {
            val jsonResult = client.postgrest.rpc(
                function = "accept_invitation",
                parameters = kotlinx.serialization.json.buildJsonObject {
                    put("p_token", token)
                },
            ).data

            parseResolutionJson(jsonResult)
        }.also { if (it.isFailure) Log.e(TAG, "acceptInvitationByToken failed", it.exceptionOrNull()) }

    override suspend fun acceptInvitationById(invitationId: String): Result<InvitationResolution> =
        runCatching {
            // Fetch token first, then accept by token
            val dto = client.postgrest[TABLE]
                .select { filter { eq("id", invitationId) } }
                .decodeSingle<FamilyInvitationDto>()
            acceptInvitationByToken(dto.token).getOrThrow()
        }.also { if (it.isFailure) Log.e(TAG, "acceptInvitationById failed", it.exceptionOrNull()) }

    override suspend fun declineInvitation(invitationId: String): Result<Unit> =
        runCatching {
            client.postgrest[TABLE]
                .update({ set("status", "DECLINED") }) {
                    filter { eq("id", invitationId) }
                }
            Unit
        }.also { if (it.isFailure) Log.e(TAG, "declineInvitation failed", it.exceptionOrNull()) }

    override suspend fun revokeInvitation(invitationId: String): Result<Unit> =
        runCatching {
            client.postgrest[TABLE]
                .update({ set("status", "REVOKED") }) {
                    filter { eq("id", invitationId) }
                }
            Unit
        }.also { if (it.isFailure) Log.e(TAG, "revokeInvitation failed", it.exceptionOrNull()) }

    // -----------------------------------------------------------------------
    // Resolve (metadata without accepting)
    // -----------------------------------------------------------------------

    override suspend fun resolveInvitationByToken(token: String): Result<InvitationResolution> =
        runCatching {
            val jsonResult = client.postgrest.rpc(
                function = "resolve_invitation",
                parameters = kotlinx.serialization.json.buildJsonObject {
                    put("p_token", token)
                },
            ).data

            parseResolutionJson(jsonResult)
        }.also { if (it.isFailure) Log.e(TAG, "resolveInvitationByToken failed", it.exceptionOrNull()) }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun parseResolutionJson(raw: String): InvitationResolution {
        return try {
            // The RPC returns a JSON object (or array with one element)
            val trimmed = raw.trim()
            val obj: JsonObject = if (trimmed.startsWith("[")) {
                kotlinx.serialization.json.Json.parseToJsonElement(trimmed)
                    .jsonObject // try as object first if single element
            } else {
                kotlinx.serialization.json.Json.parseToJsonElement(trimmed).jsonObject
            }

            val ok = obj["ok"]?.jsonPrimitive?.booleanOrNull ?: false
            InvitationResolution(
                ok            = ok,
                error         = obj["error"]?.jsonPrimitive?.contentOrNull,
                invitationId  = obj["invitation_id"]?.jsonPrimitive?.contentOrNull,
                familyId      = obj["family_id"]?.jsonPrimitive?.contentOrNull,
                familyName    = obj["family_name"]?.jsonPrimitive?.contentOrNull,
                invitedBy     = obj["invited_by"]?.jsonPrimitive?.contentOrNull,
                role          = obj["role"]?.jsonPrimitive?.contentOrNull,
                status        = obj["status"]?.jsonPrimitive?.contentOrNull,
                expiresAt     = obj["expires_at"]?.jsonPrimitive?.contentOrNull,
                alreadyMember = obj["already_member"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseResolutionJson error for: $raw", e)
            InvitationResolution(ok = false, error = "parse_error")
        }
    }

    private fun FamilyInvitationDto.toDomain() = FamilyInvitation(
        id            = id,
        familyId      = familyId,
        token         = token,
        createdBy     = createdBy,
        inviteeEmail  = inviteeEmail,
        inviteeUserId = inviteeUserId,
        status        = status,
        role          = role,
        expiresAt     = expiresAt,
        createdAt     = createdAt,
    )
}

// DTO for insert (excludes auto-generated fields)
@kotlinx.serialization.Serializable
private data class FamilyInvitationInsert(
    @kotlinx.serialization.SerialName("family_id")  val familyId: String,
    @kotlinx.serialization.SerialName("created_by") val createdBy: String,
    val role: MembershipRole,
    // expires_at is a computed expression; Supabase default handles it.
    // We pass expiresInDays only if we need to override.
    @kotlinx.serialization.Transient val expiresInDays: Int = 7,
)
