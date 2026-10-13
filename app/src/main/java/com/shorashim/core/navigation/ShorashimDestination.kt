package com.shorashim.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.shorashim.R

sealed class ShorashimDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    data object FamilyTree : ShorashimDestination(
        route = ShorashimRoutes.FAMILY_TREE,
        labelRes = R.string.nav_family_tree,
        icon = Icons.Outlined.Park,
    )

    data object Community : ShorashimDestination(
        route = ShorashimRoutes.COMMUNITY_TREES,
        labelRes = R.string.nav_community,
        icon = Icons.Outlined.Groups,
    )

    data object Memorials : ShorashimDestination(
        route = ShorashimRoutes.MEMORIALS,
        labelRes = R.string.nav_memorials,
        icon = Icons.Outlined.CalendarMonth,
    )

    data object Tehillim : ShorashimDestination(
        route = ShorashimRoutes.TEHILLIM,
        labelRes = R.string.nav_tehillim,
        icon = Icons.AutoMirrored.Outlined.MenuBook,
    )

    data object Settings : ShorashimDestination(
        route = ShorashimRoutes.SETTINGS,
        labelRes = R.string.nav_settings,
        icon = Icons.Outlined.Settings,
    )

    companion object {
        val bottomBarItems: List<ShorashimDestination> = listOf(
            FamilyTree,
            Community,
            Memorials,
            Tehillim,
        )
    }
}
