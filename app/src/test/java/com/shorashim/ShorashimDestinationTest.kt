package com.shorashim

import com.shorashim.core.navigation.ShorashimRoutes
import org.junit.Assert.assertEquals
import org.junit.Test

class ShorashimDestinationTest {
    @Test
    fun bottomBarHasFourUniqueRoutes() {
        val routes = ShorashimRoutes.bottomBarRoutes
        assertEquals(4, routes.size)
        assertEquals(routes.size, routes.toSet().size)
        assertEquals(
            listOf(
                ShorashimRoutes.FAMILY_TREE,
                ShorashimRoutes.COMMUNITY_TREES,
                ShorashimRoutes.MEMORIALS,
                ShorashimRoutes.TEHILLIM,
            ),
            routes,
        )
    }

    @Test
    fun topBarHasFourUniqueRoutes() {
        val routes = ShorashimRoutes.topBarRoutes
        assertEquals(4, routes.size)
        assertEquals(routes.size, routes.toSet().size)
        assertEquals(
            listOf(
                ShorashimRoutes.FRIENDS,
                ShorashimRoutes.HELP,
                ShorashimRoutes.DONATIONS,
                ShorashimRoutes.PROFILE,
            ),
            routes,
        )
    }
}
