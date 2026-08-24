package com.chathala.hala.core.config

import androidx.annotation.DrawableRes
import com.chathala.hala.R

/**
 * إعدادات شعار التطبيق — مكان واحد لتغيير الشعار في كل الشاشات.
 *
 * لاستبدال الشعار:
 * 1. ضع ملف PNG في: app/src/main/res/drawable/hala_logo.png
 * 2. غيّر defaultLogoRes إلى: R.drawable.hala_logo
 * 3. tinted = true  لو الشعار أبيض/أسود ويجب تلوينه بلون البراند
 *    tinted = false لو الشعار متعدد الألوان (يعرض كما هو)
 *
 * افتراضياً (null) يستخدم أيقونة دردشة من Material.
 *
 * **`hala_logo_official` مشتقّ من أيقونة المشغّل نفسها** لا رسمٌ مستقلّ، فشاشات
 * الدخول تعرض ما يراه المستخدم على شاشته. ولمَ ملفّ نقطيّ بدل `R.mipmap.ic_launcher`
 * مباشرةً؟ لأن أيقونة المشغّل أيقونة تكيّفية (adaptive-icon XML) و`painterResource`
 * لا يدعم غير المتّجهات والصور النقطية فيرمي استثناءً عندها.
 *
 * وله هامش داخلي متعمَّد: شارة الهيدر تقصّ الشعار في **دائرة**، وبلاطة الأيقونة
 * المربّعة كانت تفقد «Hala !» من زاويتها. الهامش يُبقي المحتوى كلّه داخل الدائرة
 * المحاطة، والزوايا مملوءة بلون البلاطة فلا تظهر حوافّ.
 */
object LogoConfig {
    @DrawableRes
    val defaultLogoRes: Int? = R.drawable.hala_logo_official

    const val tinted: Boolean = false
}
