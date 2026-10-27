package com.chathala.hala.feature.premium.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * صفحة الاشتراك **ورقةً** فوق الشاشة الحالية: كان التنقّل يُظهر رجوعاً وإغلاقاً معاً
 * وشريط التبويبات فوق «استعادة المشتريات».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    onDismiss: () -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxHeight(0.96f)) {
            SubscriptionScreen(
                onBack = onDismiss,
                onOpenTerms = { onDismiss(); onOpenTerms() },
                onOpenPrivacy = { onDismiss(); onOpenPrivacy() },
                asSheet = true
            )
        }
    }
}
