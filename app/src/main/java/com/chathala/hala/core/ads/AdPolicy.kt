package com.chathala.hala.core.ads

internal fun adEligibleUser(user: com.chathala.hala.feature.user.data.User?): Boolean =
    user != null && user.premiumStatusKnown && !user.isPremium

internal enum class DiscoveryAdOpportunity { NONE, NATIVE, INTERSTITIAL }

/** One opportunity per swipe: a sponsored card and a fullscreen ad must never compete. */
internal fun discoveryAdOpportunity(swipes: Int): DiscoveryAdOpportunity = when {
    swipes <= 0 -> DiscoveryAdOpportunity.NONE
    swipes % AdConfig.NATIVE_EVERY_CARDS == 0 -> DiscoveryAdOpportunity.NATIVE
    swipes % AdConfig.DISCOVER_INTERSTITIAL_EVERY_CARDS == 0 -> DiscoveryAdOpportunity.INTERSTITIAL
    else -> DiscoveryAdOpportunity.NONE
}

internal fun canRequestAds(userEligible: Boolean, consentAllowsRequests: Boolean, sdkReady: Boolean): Boolean =
    userEligible && consentAllowsRequests && sdkReady

internal fun isFreshAd(loadedAt: Long, now: Long, ttl: Long): Boolean = loadedAt > 0 && now >= loadedAt && now - loadedAt < ttl

internal fun canShowInterstitial(now: Long, foregroundStartedAt: Long, lastShownAt: Long): Boolean =
    now >= foregroundStartedAt && now - foregroundStartedAt >= AdConfig.INTERSTITIAL_WARMUP_MS &&
        (lastShownAt == 0L || now < lastShownAt || now - lastShownAt >= AdConfig.INTERSTITIAL_MIN_INTERVAL_MS)

/** هل بقي من سقف اليوم؟ العدّ يُصفَّر حين يتغيّر [today] عن يوم آخر عرض. */
internal fun underDailyCap(today: String, shownDay: String?, shownCount: Int): Boolean =
    shownDay != today || shownCount < AdConfig.INTERSTITIAL_DAILY_CAP
