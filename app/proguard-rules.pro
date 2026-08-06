# قواعد R8 لنسخة الإصدار.
# أغلب المكتبات (OkHttp/Retrofit/Coil/Firebase/Billing/Facebook) تشحن قواعدها
# الخاصة تلقائياً، فما هنا هو ما لا تستطيع المكتبات معرفته عن كودنا نحن.

# ── أثر التتبّع ─────────────────────────────────────────────
# نُبقي أرقام الأسطر ونُخفي أسماء الملفات: تقارير الأعطال في Play Console
# تبقى قابلة للقراءة دون كشف بنية المصدر.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Moshi ───────────────────────────────────────────────────
# حرِج: ApiClient يضيف KotlinJsonAdapterFactory، أي أن نماذج الشبكة تُقرأ
# بالانعكاس من أسماء الحقول ومن kotlin.Metadata. لو غيّر R8 الأسماء أو حذف
# البيانات الوصفية، تفشل كل استجابة JSON صامتةً وقت التشغيل لا وقت البناء.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class kotlin.Metadata { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.squareup.moshi.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }

# نماذج البيانات في التطبيق — تُبنى كلها بالانعكاس عبر Moshi.
-keep class com.chathala.hala.**.data.** { *; }
-keep class com.chathala.hala.core.network.** { *; }

# محوّل مخصّص يُشار إليه بالاسم في ApiClient
-keep class com.chathala.hala.feature.chats.data.MessageSenderAdapterFactory { *; }

# ── Socket.IO / Engine.IO ───────────────────────────────────
# لا تشحن قواعد consumer، وتستخدم الانعكاس داخلياً في معالجة الأحداث.
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-keep class okio.** { *; }
-dontwarn okio.**

# ── تحذيرات مكتبات لا نستخدمها فعلياً ──────────────────────
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
