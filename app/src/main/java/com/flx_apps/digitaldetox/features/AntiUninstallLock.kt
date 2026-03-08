package com.flx_apps.digitaldetox.features

import java.util.concurrent.TimeUnit

/**
 * Pure-Kotlin helper that encapsulates the time-based lock logic used by
 * [AntiUninstallFeature].  All parameters are explicit, so the functions have
 * no Android-framework dependencies and can be exercised in plain JVM unit tests.
 */
object AntiUninstallLock {

    /**
     * Returns `true` when the anti-uninstall protection is currently active.
     *
     * @param activated  whether the [AntiUninstallFeature] is switched on.
     * @param lockedUntil epoch-millisecond timestamp after which the lock expires.
     *                    `0` means "no lock set".
     * @param nowMs      current epoch-millisecond time (defaults to [System.currentTimeMillis]).
     */
    fun isLocked(
        activated: Boolean,
        lockedUntil: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean = activated && nowMs < lockedUntil

    /**
     * Calculates the epoch-millisecond timestamp at which a lock set *now* would
     * expire if the duration is [days] days.
     *
     * @param days   number of days the lock should last (must be > 0).
     * @param nowMs  starting point for the calculation (defaults to [System.currentTimeMillis]).
     * @return the expiry timestamp in epoch milliseconds.
     */
    fun expiryForDays(days: Int, nowMs: Long = System.currentTimeMillis()): Long =
        nowMs + TimeUnit.DAYS.toMillis(days.toLong())
}
