package com.chathala.hala

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.chathala.hala.core.i18n.LocaleManager
import com.chathala.hala.core.storage.AppLanguage
import com.chathala.hala.core.storage.AppTheme
import com.chathala.hala.feature.push.PushIntentCoordinator
import com.chathala.hala.navigation.HalaNavGraph
import com.chathala.hala.ui.components.OfflineBanner
import com.chathala.hala.ui.theme.HalaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // edge-to-edge: نتحكم نحن بحواف النظام (insets) — مطلوب على Android 15+.
        // enableEdgeToEdge بدل setDecorFitsSystemWindows وحده: يضبط أيضاً شفافية
        // أشرطة النظام وتباين أيقوناتها بطريقة متوافقة مع ما قبل أندرويد 15، بدل
        // setStatusBarColor/setNavigationBarColor المهجورَين. يُستدعى قبل super.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = applicationContext as HalaApp
        PushIntentCoordinator.handle(intent)
        setContent {
            val theme by app.appPreferences.theme.collectAsState(initial = AppTheme.SYSTEM)
            val language by app.appPreferences.language.collectAsState(initial = LocaleManager.current)

            // مزامنة الحالة العامة التي يقرأها S.get في كل طبقات التطبيق
            LaunchedEffect(language) { LocaleManager.setLanguage(language) }

            val direction =
                if (language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr

            HalaTheme(themeMode = theme) {
                // نتحكّم بالاتجاه فقط، ولا نتجاوز LocalContext/LocalConfiguration عمداً:
                // الحوارات والأوراق (Dialog/Popup/ModalBottomSheet) تُنشئ نافذة جديدة
                // تُعيد توفير هذين المحلّيين من الـ Activity، فكانت نصوصها ترجع للغة الجهاز.
                // كل النصوص تمرّ عبر S.get الذي يقرأ LocaleManager مباشرةً بدل CompositionLocal.
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            // لا نستخدم key(language): كان سيُعيد إنشاء NavController ويمسح
                            // مكدس التنقّل. التحديث الفوري مضمون لأن S.get يقرأ
                            // LocaleManager.current (حالة Compose) فيُعاد تركيب كل نص وحده.
                            OfflineBanner()
                            HalaNavGraph()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushIntentCoordinator.handle(intent)
    }
}
