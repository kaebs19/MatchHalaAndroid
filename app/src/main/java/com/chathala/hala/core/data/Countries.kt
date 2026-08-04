package com.chathala.hala.core.data

import com.chathala.hala.core.i18n.S

data class Country(
    val code: String,
    val nameAr: String,
    val nameEn: String,
    val flag: String
) {
    /**
     * الاسم المعروض حسب اللغة الحالية.
     *
     * مقروء وقت العرض لا وقت الإنشاء: [list] تُهيّأ مرة واحدة عند تحميل الصنف،
     * فلو خزّنّا الاسم المترجم داخلها لتجمّد على لغة الإقلاع.
     */
    val name: String get() = S.pick(nameAr, nameEn)
}

object Countries {
    val list: List<Country> = listOf(
        Country("SA", "السعودية", "Saudi Arabia", "🇸🇦"),
        Country("AE", "الإمارات", "United Arab Emirates", "🇦🇪"),
        Country("KW", "الكويت", "Kuwait", "🇰🇼"),
        Country("QA", "قطر", "Qatar", "🇶🇦"),
        Country("BH", "البحرين", "Bahrain", "🇧🇭"),
        Country("OM", "عُمان", "Oman", "🇴🇲"),
        Country("YE", "اليمن", "Yemen", "🇾🇪"),
        Country("JO", "الأردن", "Jordan", "🇯🇴"),
        Country("LB", "لبنان", "Lebanon", "🇱🇧"),
        Country("PS", "فلسطين", "Palestine", "🇵🇸"),
        Country("SY", "سوريا", "Syria", "🇸🇾"),
        Country("IQ", "العراق", "Iraq", "🇮🇶"),
        Country("EG", "مصر", "Egypt", "🇪🇬"),
        Country("LY", "ليبيا", "Libya", "🇱🇾"),
        Country("TN", "تونس", "Tunisia", "🇹🇳"),
        Country("DZ", "الجزائر", "Algeria", "🇩🇿"),
        Country("MA", "المغرب", "Morocco", "🇲🇦"),
        Country("SD", "السودان", "Sudan", "🇸🇩"),
        Country("SO", "الصومال", "Somalia", "🇸🇴"),
        Country("MR", "موريتانيا", "Mauritania", "🇲🇷"),
        Country("DJ", "جيبوتي", "Djibouti", "🇩🇯"),
        Country("KM", "جزر القمر", "Comoros", "🇰🇲"),
        Country("TR", "تركيا", "Turkey", "🇹🇷"),
        Country("IR", "إيران", "Iran", "🇮🇷"),
        Country("PK", "باكستان", "Pakistan", "🇵🇰"),
        Country("IN", "الهند", "India", "🇮🇳"),
        Country("ID", "إندونيسيا", "Indonesia", "🇮🇩"),
        Country("MY", "ماليزيا", "Malaysia", "🇲🇾"),
        Country("US", "الولايات المتحدة", "United States", "🇺🇸"),
        Country("GB", "بريطانيا", "United Kingdom", "🇬🇧"),
        Country("CA", "كندا", "Canada", "🇨🇦"),
        Country("DE", "ألمانيا", "Germany", "🇩🇪"),
        Country("FR", "فرنسا", "France", "🇫🇷"),
        Country("ES", "إسبانيا", "Spain", "🇪🇸"),
        Country("IT", "إيطاليا", "Italy", "🇮🇹"),
        Country("OTHER", "أخرى", "Other", "🌍")
    )

    fun byCode(code: String?): Country? {
        if (code.isNullOrBlank()) return null
        return list.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
