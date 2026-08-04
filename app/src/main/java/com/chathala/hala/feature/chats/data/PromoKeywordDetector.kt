package com.chathala.hala.feature.chats.data

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

data class PromoKeyword(
    val pattern: String,
    val category: String,
    val isRegex: Boolean = false
)

object PromoKeywordDetector {
    private val builtInPatterns = listOf(
        Pair(Regex("\\bsnap(?:chat)?\\b", RegexOption.IGNORE_CASE), S.get(R.string.promo_cat_snapchat)),
        Pair(Regex("سناب"), S.get(R.string.promo_cat_snapchat)),
        Pair(Regex("\\binsta(?:gram)?\\b", RegexOption.IGNORE_CASE), S.get(R.string.promo_cat_instagram)),
        Pair(Regex("[أإاآ]نست"), S.get(R.string.promo_cat_instagram)),
        Pair(Regex("\\bwhats?app\\b", RegexOption.IGNORE_CASE), S.get(R.string.promo_cat_whatsapp)),
        Pair(Regex("واتس[اأ]?[بپ]"), S.get(R.string.promo_cat_whatsapp)),
        Pair(Regex("\\btelegram\\b", RegexOption.IGNORE_CASE), S.get(R.string.promo_cat_telegram)),
        Pair(Regex("ت[يى]?ل[يى]?[جغقك]رام"), S.get(R.string.promo_cat_telegram)),
        Pair(Regex("\\btiktok\\b", RegexOption.IGNORE_CASE), S.get(R.string.promo_cat_tiktok)),
        Pair(Regex("ت[يى]?ك.*?توك"), S.get(R.string.promo_cat_tiktok)),
        Pair(Regex("\\d{8,}"), S.get(R.string.promo_cat_phone)),
    )

    private val dynamicPatterns = mutableListOf<Pair<Regex, String>>()

    fun hasPromoContent(text: String): Boolean =
        (builtInPatterns + dynamicPatterns).any { (regex, _) -> regex.containsMatchIn(text) }

    fun getMatchedCategory(text: String): String? {
        for ((regex, category) in builtInPatterns + dynamicPatterns) {
            if (regex.containsMatchIn(text)) return category
        }
        return null
    }

    fun updateFromServer(keywords: List<PromoKeyword>) {
        dynamicPatterns.clear()
        keywords.forEach { kw ->
            val regex = try {
                if (kw.isRegex) Regex(kw.pattern, RegexOption.IGNORE_CASE)
                else Regex(Regex.escape(kw.pattern), RegexOption.IGNORE_CASE)
            } catch (e: Exception) { null }
            if (regex != null) {
                dynamicPatterns.add(Pair(regex, kw.category))
            }
        }
    }
}
