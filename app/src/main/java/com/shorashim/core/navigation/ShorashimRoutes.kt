package com.shorashim.core.navigation

object ShorashimRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"

    const val FAMILY_TREE = "family_tree"
    const val CREATE_FAMILY = "create_family"
    const val FAMILY_MANAGEMENT = "family_management/{familyId}"
    fun familyManagement(familyId: String) = "family_management/$familyId"

    const val ADD_EDIT_PERSON = "add_edit_person/{familyId}?personId={personId}"
    fun addPerson(familyId: String) = "add_edit_person/$familyId"
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

    val bottomBarRoutes: List<String> = listOf(
        FAMILY_TREE,
        MEMORIALS,
        TEHILLIM,
        SETTINGS,
    )
}
