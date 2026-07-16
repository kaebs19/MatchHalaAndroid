package com.chathala.hala.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chathala.hala.HalaApp
import com.chathala.hala.R
import com.chathala.hala.ui.components.AuthScaffold
import com.chathala.hala.ui.components.HalaPrimaryButton

/**
 * شاشة الترحيب/التهنئة بعد إنشاء حساب جديد (تسجيل بريد أو أول دخول عبر Google/Apple).
 * تُظهر اسم المستخدم القادم من مزوّد الدخول ثم تقوده لإكمال ملفه.
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HalaApp
    val user by app.userRepository.currentUser.collectAsStateWithLifecycle(initialValue = null)

    val firstName = user?.name
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.substringBefore(' ')

    AuthScaffold(
        title = stringResource(R.string.welcome_title),
        subtitle = stringResource(R.string.welcome_subtitle)
    ) {
        Text(
            text = firstName
                ?.let { stringResource(R.string.welcome_greeting_named, it) }
                ?: stringResource(R.string.welcome_greeting),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        WelcomePoint(Icons.Filled.CheckCircle, stringResource(R.string.welcome_point_profile))
        Spacer(Modifier.height(16.dp))
        WelcomePoint(Icons.Rounded.Interests, stringResource(R.string.welcome_point_interests))
        Spacer(Modifier.height(16.dp))
        WelcomePoint(Icons.Rounded.Lock, stringResource(R.string.welcome_point_privacy))

        Spacer(Modifier.height(36.dp))

        HalaPrimaryButton(
            text = stringResource(R.string.welcome_continue),
            onClick = onContinue
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun WelcomePoint(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
