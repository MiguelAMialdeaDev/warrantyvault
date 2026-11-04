package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.util.PremiumValidity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class PremiumValidityTest {

    private val now = TimeUnit.DAYS.toMillis(20_000)

    @Test
    fun `premium confirmed recently is active`() {
        assertTrue(PremiumValidity.isActive(true, now - TimeUnit.DAYS.toMillis(3), now))
    }

    @Test
    fun `premium not confirmed for more than fourteen days expires`() {
        assertFalse(PremiumValidity.isActive(true, now - TimeUnit.DAYS.toMillis(15), now))
    }

    @Test
    fun `premium confirmed exactly fourteen days ago is still active`() {
        assertTrue(PremiumValidity.isActive(true, now - PremiumValidity.MAX_UNVERIFIED_MILLIS, now))
    }

    @Test
    fun `a confirmation date in the future does not count`() {
        // Retrasar el reloj del móvil no debe alargar el premium.
        assertFalse(PremiumValidity.isActive(true, now + TimeUnit.DAYS.toMillis(1), now))
    }

    @Test
    fun `without the flag there is no premium`() {
        assertFalse(PremiumValidity.isActive(false, now, now))
    }

    @Test
    fun `a flag that was never confirmed is not active`() {
        assertFalse(PremiumValidity.isActive(true, 0L, now))
    }
}
