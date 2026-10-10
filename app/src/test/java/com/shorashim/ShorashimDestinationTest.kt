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
                ShorashimRoutes.MEMORIALS,
                ShorashimRoutes.TEHILLIM,
                ShorashimRoutes.SETTINGS,
            ),
            routes,
        )
    }
}
