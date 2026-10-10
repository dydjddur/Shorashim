package com.shorashim.core.model

/**
 * Domain types used across features. Persistence begins in Phase 3.
 * People and relationships are separate entities on purpose.
 */

enum class MembershipRole {
    Owner,
    Administrator,
    Editor,
    Viewer,
}

enum class MembershipStatus {
    Active,
    Invited,
    Removed,
}

enum class LifeStatus {
    Living,
    Deceased,
}

enum class RelationshipType {
    Parent,
    Child,
    Spouse,
    Sibling,
}

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
