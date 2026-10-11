package com.shorashim.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain and DTO types used across Shorashim features.
 * Real PostgreSQL persistence and RLS active in Phase 3.
 */

@Serializable
enum class MembershipRole {
    @SerialName("OWNER")
    OWNER,
    @SerialName("ADMIN")
    ADMIN,
    @SerialName("CONTRIBUTOR")
    CONTRIBUTOR,
    @SerialName("EDITOR") // Backwards-compatible alias for CONTRIBUTOR
    EDITOR,
    @SerialName("VIEWER")
    VIEWER,
}

@Serializable
enum class FamilyStatus {
    @SerialName("DRAFT")
    DRAFT,
    @SerialName("PUBLISHED")
    PUBLISHED,
    @SerialName("ARCHIVED")
    ARCHIVED,
}

@Serializable
enum class LifeStatus {
    @SerialName("LIVING")
    LIVING,
    @SerialName("DECEASED")
    DECEASED,
}

@Serializable
enum class DatePrecision {
    @SerialName("FULL_DATE")
    FULL_DATE,
    @SerialName("YEAR_MONTH")
    YEAR_MONTH,
    @SerialName("YEAR_ONLY")
    YEAR_ONLY,
    @SerialName("UNKNOWN")
    UNKNOWN,
}

@Serializable
enum class RelationshipType {
    @SerialName("PARENT_CHILD")
    PARENT_CHILD,
    @SerialName("SPOUSE")
    SPOUSE,
}

// ---------------------------------------------------------------------------
// DTOs & Models for Supabase Postgrest
// ---------------------------------------------------------------------------

@Serializable
data class FamilyDto(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("owner_id")
    val ownerId: String = "",
    val status: FamilyStatus = FamilyStatus.DRAFT,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
)

@Serializable
data class FamilyCreateRequest(
    val name: String,
    @SerialName("owner_id")
    val ownerId: String,
    val status: FamilyStatus = FamilyStatus.DRAFT,
    val description: String? = null,
)

@Serializable
data class Family(
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val status: FamilyStatus = FamilyStatus.DRAFT,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val personCount: Int = 0,
    val currentUserRole: MembershipRole = MembershipRole.OWNER,
) {
    val isOwner: Boolean get() = currentUserRole == MembershipRole.OWNER
    val canManageMembers: Boolean get() = currentUserRole == MembershipRole.OWNER || currentUserRole == MembershipRole.ADMIN
    val canEditTreeContent: Boolean get() = currentUserRole != MembershipRole.VIEWER
    val isDraft: Boolean get() = status == FamilyStatus.DRAFT
}

@Serializable
data class FamilyMemberDto(
    @SerialName("family_id")
    val familyId: String = "",
    @SerialName("user_id")
    val userId: String = "",
    val role: MembershipRole = MembershipRole.CONTRIBUTOR,
    @SerialName("invited_by")
    val invitedBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
data class FamilyMember(
    val familyId: String,
    val userId: String,
    val userEmail: String? = null,
    val role: MembershipRole,
    val isCurrentUser: Boolean = false,
    val createdAt: String? = null,
)

@Serializable
data class PersonInsertDto(
    @SerialName("family_id")
    val familyId: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String? = null,
    @SerialName("additional_names")
    val additionalNames: String? = null,
    @SerialName("photo_path")
    val photoPath: String? = null,
    @SerialName("life_status")
    val lifeStatus: LifeStatus,
    @SerialName("birth_date")
    val birthDate: String? = null,
    @SerialName("birth_date_precision")
    val birthDatePrecision: DatePrecision? = null,
    @SerialName("death_date")
    val deathDate: String? = null,
    @SerialName("death_date_precision")
    val deathDatePrecision: DatePrecision? = null,
    val biography: String? = null,
    @SerialName("canvas_x")
    val canvasX: Double = 0.0,
    @SerialName("canvas_y")
    val canvasY: Double = 0.0,
    @SerialName("created_by")
    val createdBy: String? = null,
)

@Serializable
data class PersonDto(
    val id: String = "",
    @SerialName("family_id")
    val familyId: String = "",
    @SerialName("first_name")
    val firstName: String = "",
    @SerialName("last_name")
    val lastName: String? = null,
    @SerialName("additional_names")
    val additionalNames: String? = null,
    @SerialName("photo_path")
    val photoPath: String? = null,
    @SerialName("life_status")
    val lifeStatus: LifeStatus = LifeStatus.LIVING,
    @SerialName("birth_date")
    val birthDate: String? = null,
    @SerialName("birth_date_precision")
    val birthDatePrecision: DatePrecision? = null,
    @SerialName("death_date")
    val deathDate: String? = null,
    @SerialName("death_date_precision")
    val deathDatePrecision: DatePrecision? = null,
    val biography: String? = null,
    @SerialName("canvas_x")
    val canvasX: Double = 0.0,
    @SerialName("canvas_y")
    val canvasY: Double = 0.0,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class Person(
    val id: String,
    val familyId: String,
    val firstName: String,
    val lastName: String? = null,
    val additionalNames: String? = null,
    val photoPath: String? = null,
    val lifeStatus: LifeStatus = LifeStatus.LIVING,
    val birthDate: String? = null,
    val birthDatePrecision: DatePrecision? = null,
    val deathDate: String? = null,
    val deathDatePrecision: DatePrecision? = null,
    val biography: String? = null,
    val canvasX: Double = 0.0,
    val canvasY: Double = 0.0,
    val createdAt: String? = null,
) {
    val fullName: String
        get() = if (lastName.isNullOrBlank()) firstName else "$firstName $lastName"
}

@Serializable
data class RelationshipInsertDto(
    @SerialName("family_id")
    val familyId: String,
    @SerialName("person_id")
    val personId: String,
    @SerialName("related_person_id")
    val relatedPersonId: String,
    @SerialName("relationship_type")
    val relationshipType: RelationshipType,
    @SerialName("created_by")
    val createdBy: String? = null,
)

@Serializable
data class RelationshipDto(
    val id: String = "",
    @SerialName("family_id")
    val familyId: String = "",
    @SerialName("person_id")
    val personId: String = "",
    @SerialName("related_person_id")
    val relatedPersonId: String = "",
    @SerialName("relationship_type")
    val relationshipType: RelationshipType = RelationshipType.PARENT_CHILD,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
data class Relationship(
    val id: String,
    val familyId: String,
    val personId: String,
    val relatedPersonId: String,
    val relationshipType: RelationshipType,
    val createdAt: String? = null,
)

// ---------------------------------------------------------------------------
// Partial Date Helper
// ---------------------------------------------------------------------------

/**
 * Encapsulates partial date handling for genealogy:
 * Full date: YYYY-MM-DD
 * Year & Month: YYYY-MM-01 (precision = YEAR_MONTH)
 * Year only: YYYY-01-01 (precision = YEAR_ONLY)
 * Unknown: null (precision = UNKNOWN)
 */
data class PartialDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val precision: DatePrecision = DatePrecision.FULL_DATE,
) {
    fun toIsoDateString(): String? {
        return when (precision) {
            DatePrecision.UNKNOWN -> null
            DatePrecision.YEAR_ONLY -> year?.let { String.format(java.util.Locale.ROOT, "%04d-01-01", it) }
            DatePrecision.YEAR_MONTH -> {
                if (year != null && month != null) {
                    String.format(java.util.Locale.ROOT, "%04d-%02d-01", year, month)
                } else null
            }
            DatePrecision.FULL_DATE -> {
                if (year != null && month != null && day != null) {
                    String.format(java.util.Locale.ROOT, "%04d-%02d-%02d", year, month, day)
                } else null
            }
        }
    }

    fun toDisplayString(): String {
        return when (precision) {
            DatePrecision.UNKNOWN -> "לא ידוע"
            DatePrecision.YEAR_ONLY -> year?.toString() ?: "לא ידוע"
            DatePrecision.YEAR_MONTH -> {
                if (year != null && month != null) String.format("%02d/%04d", month, year)
                else "לא ידוע"
            }
            DatePrecision.FULL_DATE -> {
                if (year != null && month != null && day != null) {
                    String.format("%02d/%02d/%04d", day, month, year)
                } else "לא ידוע"
            }
        }
    }

    companion object {
        fun fromIso(isoString: String?, precision: DatePrecision?): PartialDate {
            if (isoString.isNullOrBlank() || precision == null || precision == DatePrecision.UNKNOWN) {
                return PartialDate(precision = DatePrecision.UNKNOWN)
            }
            val parts = isoString.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull()
            val m = parts.getOrNull(1)?.toIntOrNull()
            val d = parts.getOrNull(2)?.toIntOrNull()

            return when (precision) {
                DatePrecision.YEAR_ONLY -> PartialDate(year = y, precision = DatePrecision.YEAR_ONLY)
                DatePrecision.YEAR_MONTH -> PartialDate(year = y, month = m, precision = DatePrecision.YEAR_MONTH)
                DatePrecision.FULL_DATE -> PartialDate(year = y, month = m, day = d, precision = DatePrecision.FULL_DATE)
                DatePrecision.UNKNOWN -> PartialDate(precision = DatePrecision.UNKNOWN)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Backward compatibility placeholders for Phase 1 & 2
// ---------------------------------------------------------------------------

data class FamilySummary(
    val familyId: String? = null,
    val name: String? = null,
    val personCount: Int = 0,
)

data class MemorialSummary(
    val upcomingCount: Int = 0,
)

data class TehillimCatalogStatus(
    val verifiedTextAvailable: Boolean = false,
    val chapterCount: Int = 0,
)

// ---------------------------------------------------------------------------
// Phase 4: Memorial Events & Profiles
// ---------------------------------------------------------------------------

@Serializable
data class MemorialEventInsertDto(
    @SerialName("family_id")
    val familyId: String,
    @SerialName("person_id")
    val personId: String? = null,
    @SerialName("person_name")
    val personName: String,
    @SerialName("event_date")
    val eventDate: String,
    @SerialName("hebrew_date")
    val hebrewDate: String? = null,
    val description: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
)

@Serializable
data class MemorialEventDto(
    val id: String = "",
    @SerialName("family_id")
    val familyId: String = "",
    @SerialName("person_id")
    val personId: String? = null,
    @SerialName("person_name")
    val personName: String = "",
    @SerialName("event_date")
    val eventDate: String = "",
    @SerialName("hebrew_date")
    val hebrewDate: String? = null,
    val description: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class MemorialEvent(
    val id: String,
    val familyId: String,
    val familyName: String = "",
    val personId: String? = null,
    val personName: String,
    val eventDate: String,
    val hebrewDate: String? = null,
    val description: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class UserProfileDto(
    val id: String = "",
    @SerialName("full_name")
    val fullName: String = "",
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class UserProfile(
    val id: String,
    val fullName: String,
    val displayName: String = "",
    val avatarUrl: String? = null,
)

// ---------------------------------------------------------------------------
// Phase 5: Family Tree Invitations
// ---------------------------------------------------------------------------

@Serializable
enum class InvitationStatus {
    @SerialName("PENDING")  PENDING,
    @SerialName("ACCEPTED") ACCEPTED,
    @SerialName("DECLINED") DECLINED,
    @SerialName("REVOKED")  REVOKED,
}

@Serializable
data class FamilyInvitationDto(
    val id: String = "",
    @SerialName("family_id")       val familyId: String = "",
    val token: String = "",
    @SerialName("created_by")      val createdBy: String = "",
    @SerialName("invitee_email")   val inviteeEmail: String? = null,
    @SerialName("invitee_user_id") val inviteeUserId: String? = null,
    val status: InvitationStatus = InvitationStatus.PENDING,
    val role: MembershipRole = MembershipRole.CONTRIBUTOR,
    @SerialName("expires_at")  val expiresAt: String? = null,
    @SerialName("created_at")  val createdAt: String? = null,
    @SerialName("updated_at")  val updatedAt: String? = null,
)

data class FamilyInvitation(
    val id: String,
    val familyId: String,
    val familyName: String = "",   // enriched client-side
    val token: String,
    val createdBy: String,
    val inviteeEmail: String? = null,
    val inviteeUserId: String? = null,
    val status: InvitationStatus,
    val role: MembershipRole,
    val expiresAt: String? = null,
    val createdAt: String? = null,
) {
    /** Deep-link URL that can be shared */
    val shareUrl: String get() = "https://shorashim-invite.vercel.app/invite/$token"

    val isPending: Boolean get() = status == InvitationStatus.PENDING
}

/** Result from resolve_invitation / accept_invitation RPCs */
data class InvitationResolution(
    val ok: Boolean,
    val error: String? = null,           // error code from the RPC
    val invitationId: String? = null,
    val familyId: String? = null,
    val familyName: String? = null,
    val invitedBy: String? = null,
    val role: String? = null,
    val status: String? = null,
    val expiresAt: String? = null,
    val alreadyMember: Boolean = false,
)

/** How a person that is being added from a tree card relates to the card's person (the "anchor"). */
enum class NewPersonRelation { PARENT, CHILD, SPOUSE, SIBLING }
