package com.errata.app.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessTokenCacheTest {

    @Test
    fun freshWithinTtl() {
        assertTrue(AccessTokenCache.isFresh(nowElapsedMs = 10_000, cachedAtElapsedMs = 1_000))
    }

    @Test
    fun staleAtTtl() {
        assertFalse(
            AccessTokenCache.isFresh(
                nowElapsedMs = AccessTokenCache.TTL_MS,
                cachedAtElapsedMs = 0,
            ),
        )
    }

    @Test
    fun clockWentBackwards_notFresh() {
        assertFalse(AccessTokenCache.isFresh(nowElapsedMs = 1_000, cachedAtElapsedMs = 5_000))
    }
}
