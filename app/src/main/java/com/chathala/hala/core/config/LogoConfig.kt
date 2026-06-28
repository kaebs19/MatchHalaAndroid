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
 */
object LogoConfig {
    @DrawableRes
    val defaultLogoRes: Int? = R.drawable.dardasha_hala_log

    const val tinted: Boolean = false
}
