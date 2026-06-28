package com.chathala.hala.core.data

data class Country(
    val code: String,
    val nameAr: String,
    val flag: String
)

object Countries {
    val list: List<Country> = listOf(
        Country("SA", "السعودية", "🇸🇦"),
        Country("AE", "الإمارات", "🇦🇪"),
        Country("KW", "الكويت", "🇰🇼"),
        Country("QA", "قطر", "🇶🇦"),
        Country("BH", "البحرين", "🇧🇭"),
        Country("OM", "عُمان", "🇴🇲"),
        Country("YE", "اليمن", "🇾🇪"),
        Country("JO", "الأردن", "🇯🇴"),
        Country("LB", "لبنان", "🇱🇧"),
        Country("PS", "فلسطين", "🇵🇸"),
        Country("SY", "سوريا", "🇸🇾"),
        Country("IQ", "العراق", "🇮🇶"),
        Country("EG", "مصر", "🇪🇬"),
        Country("LY", "ليبيا", "🇱🇾"),
        Country("TN", "تونس", "🇹🇳"),
        Country("DZ", "الجزائر", "🇩🇿"),
        Country("MA", "المغرب", "🇲🇦"),
        Country("SD", "السودان", "🇸🇩"),
        Country("SO", "الصومال", "🇸🇴"),
        Country("MR", "موريتانيا", "🇲🇷"),
        Country("DJ", "جيبوتي", "🇩🇯"),
        Country("KM", "جزر القمر", "🇰🇲"),
        Country("TR", "تركيا", "🇹🇷"),
        Country("IR", "إيران", "🇮🇷"),
        Country("PK", "باكستان", "🇵🇰"),
        Country("IN", "الهند", "🇮🇳"),
        Country("ID", "إندونيسيا", "🇮🇩"),
        Country("MY", "ماليزيا", "🇲🇾"),
        Country("US", "الولايات المتحدة", "🇺🇸"),
        Country("GB", "بريطانيا", "🇬🇧"),
        Country("CA", "كندا", "🇨🇦"),
        Country("DE", "ألمانيا", "🇩🇪"),
        Country("FR", "فرنسا", "🇫🇷"),
        Country("ES", "إسبانيا", "🇪🇸"),
        Country("IT", "إيطاليا", "🇮🇹"),
        Country("OTHER", "أخرى", "🌍")
    )

    fun byCode(code: String?): Country? {
        if (code.isNullOrBlank()) return null
        return list.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
