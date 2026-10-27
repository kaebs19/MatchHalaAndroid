package com.chathala.hala.core.ads

import org.junit.Assert.*
import org.junit.Test

class AdPolicyTest {
    @Test fun `requests wait for a known eligible user consent and sdk readiness`() {
        for (user in listOf(false, true)) for (consent in listOf(false, true)) for (ready in listOf(false, true)) {
            assertEquals(user && consent && ready, canRequestAds(user, consent, ready))
        }
    }

    @Test fun `first discovery cards never trigger an ad`() {
        (0 until AdConfig.NATIVE_EVERY_CARDS).forEach {
            assertEquals(DiscoveryAdOpportunity.NONE, discoveryAdOpportunity(it))
        }
    }

    @Test fun `sponsored and fullscreen opportunities never compete on the same swipe`() {
        assertEquals(DiscoveryAdOpportunity.NATIVE, discoveryAdOpportunity(8))
        assertEquals(DiscoveryAdOpportunity.INTERSTITIAL, discoveryAdOpportunity(20))
        assertEquals(DiscoveryAdOpportunity.NATIVE, discoveryAdOpportunity(40))
        assertEquals(DiscoveryAdOpportunity.NONE, discoveryAdOpportunity(-1))
    }

    @Test fun `no fullscreen interruption during the first foreground minute`() {
        val start = 1_000_000L
        assertFalse(canShowInterstitial(start + AdConfig.INTERSTITIAL_WARMUP_MS - 1, start, 0L))
        assertTrue(canShowInterstitial(start + AdConfig.INTERSTITIAL_WARMUP_MS, start, 0L))
    }

    @Test fun `shared frequency cap blocks early impressions and allows the exact boundary`() {
        val last = 1_000_000L
        assertFalse(canShowInterstitial(last + AdConfig.INTERSTITIAL_MIN_INTERVAL_MS - 1, 1L, last))
        assertTrue(canShowInterstitial(last + AdConfig.INTERSTITIAL_MIN_INTERVAL_MS, 1L, last))
    }

    @Test fun `clock rollback cannot display immediately after resuming`() {
        assertFalse(canShowInterstitial(100_000L, 200_000L, 300_000L))
        assertTrue(canShowInterstitial(200_000L, 1L, 300_000L))
    }

    @Test fun `expired and future loaded ads are rejected`() {
        val loaded = 1_000_000L
        assertTrue(isFreshAd(loaded, loaded, AdConfig.INTERSTITIAL_TTL_MS))
        assertTrue(isFreshAd(loaded, loaded + AdConfig.INTERSTITIAL_TTL_MS - 1, AdConfig.INTERSTITIAL_TTL_MS))
        assertFalse(isFreshAd(loaded, loaded + AdConfig.INTERSTITIAL_TTL_MS, AdConfig.INTERSTITIAL_TTL_MS))
        assertFalse(isFreshAd(loaded, loaded - 1, AdConfig.INTERSTITIAL_TTL_MS))
        assertFalse(isFreshAd(0, loaded, AdConfig.INTERSTITIAL_TTL_MS))
    }
    @Test fun `unknown subscription status and premium accounts never qualify for ads`() {
        val user = com.chathala.hala.feature.user.data.User(id = "one", name = "User", email = "one@example.test")
        assertFalse(adEligibleUser(null))
        assertFalse(adEligibleUser(user.copy(premiumStatusKnown = false)))
        assertFalse(adEligibleUser(user.copy(isPremium = true)))
        assertTrue(adEligibleUser(user))
    }


    @Test
    fun dailyCapResetsOnNewDay() {
        assertTrue(underDailyCap("20261028", null, 0))
        assertTrue(underDailyCap("20261028", "20261028", AdConfig.INTERSTITIAL_DAILY_CAP - 1))
        assertFalse(underDailyCap("20261028", "20261028", AdConfig.INTERSTITIAL_DAILY_CAP))
        assertTrue(underDailyCap("20261029", "20261028", AdConfig.INTERSTITIAL_DAILY_CAP))
    }
}
