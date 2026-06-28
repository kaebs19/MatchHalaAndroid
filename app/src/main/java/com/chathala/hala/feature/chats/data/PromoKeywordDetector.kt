package com.chathala.hala.feature.chats.data

data class PromoKeyword(
    val pattern: String,
    val category: String,
    val isRegex: Boolean = false
)

object PromoKeywordDetector {
    private val builtInPatterns = listOf(
        Pair(Regex("\\bsnap(?:chat)?\\b", RegexOption.IGNORE_CASE), "سناب شات"),
        Pair(Regex("سناب"), "سناب شات"),
        Pair(Regex("\\binsta(?:gram)?\\b", RegexOption.IGNORE_CASE), "إنستقرام"),
        Pair(Regex("[أإاآ]نست"), "إنستقرام"),
        Pair(Regex("\\bwhats?app\\b", RegexOption.IGNORE_CASE), "واتساب"),
        Pair(Regex("واتس[اأ]?[بپ]"), "واتساب"),
        Pair(Regex("\\btelegram\\b", RegexOption.IGNORE_CASE), "تيليقرام"),
        Pair(Regex("ت[يى]?ل[يى]?[جغقك]رام"), "تيليقرام"),
        Pair(Regex("\\btiktok\\b", RegexOption.IGNORE_CASE), "تيك توك"),
        Pair(Regex("ت[يى]?ك.*?توك"), "تيك توك"),
        Pair(Regex("\\d{8,}"), "رقم هاتف"),
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
