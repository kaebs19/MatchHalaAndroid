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

## نسخة الرفع للمتجر

```bash
bash scripts/build-release.sh --check   # فحص الجاهزية فقط
bash scripts/build-release.sh           # فحص + بناء AAB موقّعة
```

السكربت يفحص قبل أن يبني: رقم الإصدار، اكتمال بيانات التوقيع ووجود المفتاح،
ملاحظات المتجر في `distribution/whatsnew/` (حدّ 500 حرف لكل لغة)، وجود الـ SDK،
ونظافة شجرة git. ثم يشغّل `lintRelease` (الترجمات الناقصة تُفشل البناء) واختبارات
الوحدة، ويُخرج `app/build/outputs/bundle/release/app-release.aab`.

**قبل كل رفع:** ارفع `versionCode` في `app/build.gradle.kts` — المتجر يرفض رقماً
مساوياً أو أقلّ من آخر ما رُفع، وبقاء الرقم كما هو يجعل تقارير الانهيار غير قابلة
للردّ إلى كودها.

> لا يعمل هذا في الجلسات السحابية: مفتاح التوقيع سرّ خارج المستودع، ومستودع
> Google محجوب افتراضياً (انظر القسم التالي). البناء الموقّع يتم محلياً.

## البناء في جلسات Claude Code السحابية

الجلسة السحابية تبدأ بنسخة نظيفة بلا Android SDK، وشبكتها تسمح بقائمة نطاقات
محدودة. قائمة **Trusted** الافتراضية تشمل Maven Central وGradle **لكن لا تشمل
مستودع Google** — ومنه تُخدَم AGP وAndroidX وحزم الـ SDK نفسها. النتيجة: لا ترجمة.

### مرّة واحدة: السماح لمستودع Google

من إعدادات البيئة في [claude.ai/code](https://claude.ai/code):

```
Network access → Custom
Allowed domains:
  dl.google.com
  maven.google.com
✓ Also include default list of common package managers
```

`maven.google.com` يُعيد التوجيه إلى `dl.google.com`، فالاثنان لازمان.

### تلقائياً: تهيئة الـ SDK

`scripts/setup-android-sdk.sh` يُنزّل `platform-tools` وplatform الـ `compileSdk`
وbuild-tools، ثم يكتب `local.properties`. يعمل من خطّاف `SessionStart` في
`.claude/settings.json` مع كل جلسة، وهو:

- **قابل لإعادة التشغيل** — يتحقّق قبل أن يُنزّل.
- **لا يمسّ إعداداً محلياً** — إن كان `ANDROID_HOME` مضبوطاً (جهاز عليه Android
  Studio) يكتفي بالتحقّق.
- **لا يُفشل الجلسة أبداً** — ينتهي بصفر حتى عند تعذّر التنزيل، ويشرح السبب.

للتشغيل يدوياً: `bash scripts/setup-android-sdk.sh`

> لتفادي التنزيل مع كل جلسة، الصِق محتوى السكربت في حقل **Setup script** بإعدادات
> البيئة أيضاً — يُنفَّذ مرّة واحدة عند بناء البيئة ثم يُخزَّن في ذاكرتها المؤقتة.
