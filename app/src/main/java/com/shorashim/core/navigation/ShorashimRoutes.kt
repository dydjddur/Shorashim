package com.shorashim.core.navigation

object ShorashimRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"

    const val FAMILY_TREE = "family_tree"
    const val MEMORIALS = "memorials"
    const val TEHILLIM = "tehillim"
    const val SETTINGS = "settings"

    val bottomBarRoutes: List<String> = listOf(
        FAMILY_TREE,
        MEMORIALS,
        TEHILLIM,
        SETTINGS,
    )
}
