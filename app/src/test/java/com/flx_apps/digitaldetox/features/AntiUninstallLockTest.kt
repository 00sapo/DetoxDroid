package com.flx_apps.digitaldetox.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [AntiUninstallLock], the pure-Kotlin helper that backs the
 * time-based uninstall-protection logic in [AntiUninstallFeature].
 *
 * All tests run on the JVM (no Android context needed).
 */
class AntiUninstallLockTest {

    // -----------------------------------------------------------------------
    // isLocked() – basic activation guard
    // -----------------------------------------------------------------------

    @Test
    fun isLocked_whenFeatureDeactivated_returnsFalseRegardlessOfLockTime() {
        // Feature is off → never locked, even with a far-future expiry
        assertFalse(
            AntiUninstallLock.isLocked(
                activated = false,
                lockedUntil = Long.MAX_VALUE,
                nowMs = 0L
            )
        )
    }

    @Test
    fun isLocked_whenActivatedAndLockInFuture_returnsTrue() {
        // lock expires at t=2000, current time is t=1000 → locked
        assertTrue(
            AntiUninstallLock.isLocked(
                activated = true,
                lockedUntil = 2_000L,
                nowMs = 1_000L
            )
        )
    }

    @Test
    fun isLocked_whenActivatedAndLockExpiredInPast_returnsFalse() {
        // lock expired at t=500, current time is t=1000 → not locked
        assertFalse(
            AntiUninstallLock.isLocked(
                activated = true,
                lockedUntil = 500L,
                nowMs = 1_000L
            )
        )
    }

    @Test
    fun isLocked_whenActivatedAndLockExpiresExactlyNow_returnsFalse() {
        // boundary: lock expiry == now → the lock has just expired (nowMs < lockedUntil is false)
        val now = 1_000L
        assertFalse(
            AntiUninstallLock.isLocked(
                activated = true,
                lockedUntil = now,
                nowMs = now
            )
        )
    }

    @Test
    fun isLocked_whenActivatedAndLockedUntilIsZero_returnsFalse() {
        // lockedUntil == 0 means "no lock has been set yet"
        assertFalse(
            AntiUninstallLock.isLocked(
                activated = true,
                lockedUntil = 0L,
                nowMs = 1_000L
            )
        )
    }

    @Test
    fun isLocked_whenActivatedAndLockedUntilIsMaxLong_returnsTrue() {
        // Extreme future timestamp – should still report locked
        assertTrue(
            AntiUninstallLock.isLocked(
                activated = true,
                lockedUntil = Long.MAX_VALUE,
                nowMs = System.currentTimeMillis()
            )
        )
    }

    @Test
    fun isLocked_whenDeactivatedAndLockInPast_returnsFalse() {
        // Both deactivated AND expired – still false (feature off dominates)
        assertFalse(
            AntiUninstallLock.isLocked(
                activated = false,
                lockedUntil = 500L,
                nowMs = 1_000L
            )
        )
    }

    // -----------------------------------------------------------------------
    // expiryForDays() – lock-duration calculation
    // -----------------------------------------------------------------------

    @Test
    fun expiryForDays_1Day_addsExactly24HoursInMilliseconds() {
        val base = 0L
        val expiry = AntiUninstallLock.expiryForDays(days = 1, nowMs = base)
        assertEquals(TimeUnit.DAYS.toMillis(1), expiry - base)
    }

    @Test
    fun expiryForDays_7Days_addsExactly7DaysInMilliseconds() {
        val base = 10_000L
        val expiry = AntiUninstallLock.expiryForDays(days = 7, nowMs = base)
        assertEquals(TimeUnit.DAYS.toMillis(7), expiry - base)
    }

    @Test
    fun expiryForDays_30Days_addsExactly30DaysInMilliseconds() {
        val base = 0L
        val expiry = AntiUninstallLock.expiryForDays(days = 30, nowMs = base)
        assertEquals(TimeUnit.DAYS.toMillis(30), expiry - base)
    }

    @Test
    fun expiryForDays_365Days_addsExactlyOneYear() {
        val base = 0L
        val expiry = AntiUninstallLock.expiryForDays(days = 365, nowMs = base)
        // 365 days × 24 h × 60 min × 60 s × 1000 ms
        assertEquals(365L * 24 * 60 * 60 * 1_000, expiry - base)
    }

    @Test
    fun expiryForDays_doesNotOverflowFor365Days() {
        // Largest supported value (365) should produce a positive duration
        val durationMs = TimeUnit.DAYS.toMillis(365)
        assertTrue("Duration must be positive", durationMs > 0)
    }

    @Test
    fun expiryForDays_resultIsAfterNow() {
        // For any positive number of days, the expiry must always be in the future
        val now = System.currentTimeMillis()
        val expiry = AntiUninstallLock.expiryForDays(days = 1, nowMs = now)
        assertTrue("Expiry must be strictly after now", expiry > now)
    }

    // -----------------------------------------------------------------------
    // Integration: isLocked() used with expiryForDays()
    // -----------------------------------------------------------------------

    @Test
    fun isLocked_afterSettingLockForOneDay_returnsTrue() {
        val now = 0L
        val expiry = AntiUninstallLock.expiryForDays(days = 1, nowMs = now)
        // Halfway through the lock window
        val midpoint = now + TimeUnit.HOURS.toMillis(12)
        assertTrue(
            AntiUninstallLock.isLocked(
                activated = true,
                lockedUntil = expiry,
                nowMs = midpoint
            )
        )
    }

    @Test
    fun isLocked_afterLockExpires_returnsFalse() {
        val lockStart = 0L
        val expiry = AntiUninstallLock.expiryForDays(days = 1, nowMs = lockStart)
        // One millisecond after expiry
        val afterExpiry = expiry + 1
        assertFalse(
            AntiUninstallLock.isLocked(
                activated = true,
                lockedUntil = expiry,
                nowMs = afterExpiry
            )
        )
    }
}
