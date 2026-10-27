import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// قراءة بيانات التوقيع من ملف keystore.properties (غير مُضمّن في الكود)
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
}

android {
    namespace = "com.chathala.hala"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chathala.hala"
        minSdk = 32
        targetSdk = 36
        // مهم: الإنتاج في المتجر عند 21 (2.2.4) — تُحقّق منه بسحب الحزمة من جهاز
        // حقيقي، لا بالذاكرة. يُرفع الرقم هنا ويُدفع قبل كل بناء، ليبقى كل إصدار
        // قابلاً للتتبّع إلى كوده بالضبط (أعوزنا هذا في تشخيص انهيارات 2.2.2).
        //
        // تنبيه تاريخي: 2.2.3 (20) بُنيت من فرع لم يكن مدموجاً مع main، فشُحنت
        // ناقصةً UMP وترقية SDK الإعلانات واستعادة المشتريات. 2.2.4 حملتها.
        versionCode = 27
        versionName = "2.6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // اللغات المدعومة صراحةً — يُسقط لغات المكتبات غير المستخدمة من الحزمة
        // ويجعل قائمة اللغات في إعدادات النظام (أندرويد 13+) مطابقة لما ندعمه فعلاً.
        resourceConfigurations += listOf("ar", "en")
    }

    lint {
        // حارس آلي: نصّ جديد بلا ترجمة إنجليزية (أو العكس) يُفشل البناء بدل أن
        // يظهر بالعربية داخل واجهة إنجليزية. هذا ما يبقي اللغتين متطابقتين تلقائياً.
        error += listOf("MissingTranslation", "ExtraTranslation")
        abortOnError = true
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            val mediationTesting = providers.gradleProperty("halaMediationTest").orNull == "true"
            val adTestDevices = providers.gradleProperty("halaAdTestDeviceIds").orNull.orEmpty()
                .split(',').map(String::trim).filter(String::isNotEmpty)
            require(adTestDevices.all { it.matches(Regex("[A-Fa-f0-9]{32}")) }) { "Invalid AdMob test device ID" }
            require(!mediationTesting || adTestDevices.isNotEmpty()) { "Mediation testing requires -PhalaAdTestDeviceIds=<registered-device-id>" }
            buildConfigField("boolean", "MEDIATION_TESTING", mediationTesting.toString())
            buildConfigField("String[]", "AD_TEST_DEVICE_IDS", adTestDevices.joinToString(prefix = "new String[]{", postfix = "}") { "\"$it\"" })
        }
        release {
            buildConfigField("boolean", "MEDIATION_TESTING", "false")
            buildConfigField("String[]", "AD_TEST_DEVICE_IDS", "new String[]{}")
            // R8: تصغير الكود + إزالة الموارد غير المستخدمة. قواعد الحفظ في
            // proguard-rules.pro — انتبه أن Moshi هنا يعمل بالانعكاس، فأي نموذج
            // شبكة جديد خارج حزم data/network يحتاج قاعدة حفظ صريحة.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // ملاحظة: androidx.appcompat و com.google.android.material أُزيلا عمداً —
    // التطبيق Compose بالكامل، والقالب يرث من android:Theme.Material (إطار
    // النظام) لا من المكتبة. وجودهما كان يُدخل BottomSheetDialog/SheetDialog
    // اللذين يستدعيان setStatusBarColor/setNavigationBarColor المهجورَين في
    // أندرويد 15، فتشكو منهما مراجعة Play رغم أننا لا نستدعيهما.

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)

    implementation(libs.zxing.core)

    implementation(libs.lottie.compose)

    // Google Play Billing — الاشتراكات (premium)
    implementation(libs.billing.ktx)

    // تسجيل الدخول عبر فيسبوك
    implementation(libs.facebook.login)

    // فيديو المحادثة: ضغط على الجهاز (Transformer) وتشغيل (ExoPlayer) + تمويه الوجوه
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.media3.common)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.mlkit.face.detection)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    // Analytics: يُفعّل مقاييس المستخدمين في لوحة AdMob بعد ربط مشروع Firebase بها
    implementation(libs.firebase.analytics)
    implementation(libs.play.services.location)
    implementation(libs.play.services.ads)
    // AdMob remains the mediator; dashboard mappings supply Unity Game/Placement IDs.
    implementation(libs.unity.ads)
    implementation(libs.admob.unity.adapter)
    implementation(libs.play.review.ktx)
    implementation(libs.user.messaging.platform)

    implementation(libs.socketio.client) {
        // socket.io-client يحمل json-java قديم غير متوافق — نستبعده ونستخدم Android's org.json
        exclude(group = "org.json", module = "json")
    }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
