package com.alhaq.amnshield

import com.alhaq.amnshield.data.blockers.PackageWand
import org.junit.Assert.*
import org.junit.Test

class PackageWandTest {

    @Test
    fun testTikTokCategory() {
        val category = PackageWand.getCategoryForPackage("com.zhiliaoapp.musically")
        assertEquals("social_media", category)
    }

    @Test
    fun testGamingCategory() {
        val category = PackageWand.getCategoryForPackage("com.pubg.imobile")
        assertEquals("gaming", category)
    }

    @Test
    fun testUnknownCategory() {
        val category = PackageWand.getCategoryForPackage("com.unknown.app")
        assertNull(category)
    }
}
