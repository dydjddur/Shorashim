package com.shorashim.core.navigation

object ShorashimRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val VERIFICATION_PENDING = "verification_pending?email={email}"
    fun verificationPending(email: String = ""): String =
        if (email.isBlank()) "verification_pending"
        else "verification_pending?email=${java.net.URLEncoder.encode(email, "UTF-8")}"

    const val FAMILY_TREE = "family_tree"
    const val CREATE_FAMILY = "create_family"
    const val FAMILY_MANAGEMENT = "family_management/{familyId}"
    fun familyManagement(familyId: String) = "family_management/$familyId"

    const val ADD_EDIT_PERSON = "add_edit_person/{familyId}?personId={personId}&relation={relation}&anchorId={anchorId}"
    fun addPerson(familyId: String) = "add_edit_person/$familyId"
    fun addRelative(familyId: String, relation: String, anchorId: String) =
        "add_edit_person/$familyId?relation=$relation&anchorId=$anchorId"
    fun editPerson(familyId: String, personId: String) = "add_edit_person/$familyId?personId=$personId"

    const val MEMORIALS = "memorials"
    const val EVENTS = "memorials"
    const val TEHILLIM = "tehillim"
    const val TEHILLIM_WITH_PARAMS = "tehillim?deceasedName={deceasedName}"
    fun tehillim(deceasedName: String? = null): String {
        return if (deceasedName.isNullOrBlank()) TEHILLIM
        else "tehillim?deceasedName=${java.net.URLEncoder.encode(deceasedName, "UTF-8")}"
    }

    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val FRIENDS = "friends"
    const val DONATIONS = "donations"
    const val HELP = "help"

    // Phase 5: Invitations
    const val INVITATIONS = "invitations"
    const val INVITATION_DEEP_LINK = "invitation/{token}"
    fun invitationDeepLink(token: String) = "invitation/$token"

    // Phase 8: E2EE Chat
    const val CHATS = "chats"
    const val CHAT_CONVERSATION = "chat_conversation/{otherUserId}?otherUserName={otherUserName}"
    fun chatConversation(otherUserId: String, otherUserName: String = ""): String {
        val encodedName = java.net.URLEncoder.encode(otherUserName, "UTF-8")
        return "chat_conversation/$otherUserId?otherUserName=$encodedName"
    }

    // Phase 7: Community Family Trees
    const val COMMUNITY_TREES = "community_trees"
    const val VIEW_COMMUNITY_TREE = "view_community_tree/{familyId}"
    fun viewCommunityTree(familyId: String) = "view_community_tree/$familyId"

    val bottomBarRoutes: List<String> = listOf(
        FAMILY_TREE,
        COMMUNITY_TREES,
        MEMORIALS,
        TEHILLIM,
    )

    val topBarRoutes: List<String> = listOf(
        FRIENDS,
        HELP,
        DONATIONS,
        PROFILE,
    )
}

