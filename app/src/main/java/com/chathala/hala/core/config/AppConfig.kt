package com.chathala.hala.core.config

/**
 * إعدادات التطبيق الثابتة — URLs, حدود validation, مفاتيح خارجية.
 * مكان واحد لتعديل أي قيمة.
 */
object AppConfig {
    const val BASE_URL = "https://matchhala.chathala.com/"
    const val PRIVACY_URL = "https://matchhala.chathala.com/privacy"
    const val TERMS_URL = "https://matchhala.chathala.com/terms"
    const val SUPPORT_EMAIL = "support@chathala.com"

    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_NAME_LENGTH = 2
    const val MAX_NAME_LENGTH = 40
    const val MIN_AGE = 18
    const val MAX_AGE = 90
    const val MAX_INTERESTS = 10
    const val MIN_INTERESTS = 1

    // Web Client ID — Google Cloud Console (OAuth 2.0 Client IDs → Web application)
    // يجب أن يطابق GOOGLE_WEB_CLIENT_ID في .env على السيرفر.
    const val GOOGLE_WEB_CLIENT_ID = "145789132333-ja330hh16706ovq9n6ka921ld18prmda.apps.googleusercontent.com"
}
