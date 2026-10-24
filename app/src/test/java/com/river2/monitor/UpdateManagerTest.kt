package com.river2.monitor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {
    @Test
    fun isVersionNewer_compares_numeric_components() {
        assertTrue(UpdateManager.isVersionNewer("3.1.0", "3.0.0"))
        assertTrue(UpdateManager.isVersionNewer("1.10.0", "1.9.9"))
        assertFalse(UpdateManager.isVersionNewer("3.0.0", "3.0.0"))
        assertFalse(UpdateManager.isVersionNewer("3.0.9", "3.1.0"))
    }
}
