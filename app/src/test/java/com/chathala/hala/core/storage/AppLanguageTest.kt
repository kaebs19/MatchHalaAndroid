package com.chathala.hala.core.storage

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * كشف لغة الجهاز عند أول تشغيل.
 *
 * تعذّر اختباره على المحاكي (بناء إنتاجي لا يقبل تغيير `persist.sys.locale`)،
 * وهو منطق يُنفَّذ مرة واحدة في عمر التثبيت — فالانحدار فيه يمرّ بلا أن يُلحظ.
 */
class AppLanguageTest {

    private lateinit var original: Locale

    @Before fun save() { original = Locale.getDefault() }
    @After fun restore() { Locale.setDefault(original) }

    @Test
    fun `الاختيار المحفوظ يسبق لغة الجهاز`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromString("ARABIC"))

        Locale.setDefault(Locale("ar"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromString("ENGLISH"))
    }

    @Test
    fun `بلا اختيار محفوظ نتبع لغة الجهاز`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromString(null))

        Locale.setDefault(Locale("ar", "SA"))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromString(null))
    }

    @Test
    fun `لغة ثالثة تقع على العربية لا الإنجليزية`() {
        // جمهور التطبيق عربي: الفرنسي يرى العربية لا الإنجليزية
        Locale.setDefault(Locale.FRENCH)
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromString(null))
    }

    @Test
    fun `قيمة محفوظة تالفة لا تُسقط التطبيق`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromString("GIBBERISH"))
    }
}
