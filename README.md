# MatchHalaAndroid

تطبيق **هلا (Hala)** للأندرويد — تطبيق دردشة وتعارف.

- **الحزمة:** `com.chathala.hala`
- **التقنية:** Kotlin · Jetpack Compose · MVVM · Retrofit · Socket.IO · Coil
- **الخادم:** [MatchHalaApi](https://github.com/kaebs19) (Node/Express)
- **iOS:** MatchHalaApp (مشروع منفصل)

## المزايا الرئيسية
- الدردشة اللحظية (Socket.IO) مع السحب للردّ، تجميع الرسائل، والمحتوى المؤقت
- الاكتشاف والتعارف (Swipe) والبحث والفلاتر المدفوعة
- الإشعارات اللحظية (FCM)
- نظام تقييد المراسلة والمراجعة + شفافية حالة الحساب
- إعدادات الخصوصية والإشعارات والاكتشاف

## البناء
```bash
JAVA_HOME=".../Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

> ملاحظة: `keystore.properties` ومفتاح التوقيع `*.jks` غير مُضمّنين (أسرار) — يلزمان للبناء الموقّع.
