package com.chathala.hala.ui.components

import com.chathala.hala.R

import com.chathala.hala.core.i18n.S

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FacebookBlue = Color(0xFF1877F2)

@Composable
fun FacebookSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = FacebookBlue,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                FacebookLogoF()
                Spacer(Modifier.size(12.dp))
                Text(
                    text = S.get(R.string.btn_facebook_continue),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

/**
 * شعار فيسبوك بسيط (حرف f أزرق داخل دائرة بيضاء).
 * يُثبّت اتجاه LTR لتفادي انعكاس الحرف في واجهة RTL — مثل [GoogleSignInButton].
 */
@Composable
private fun FacebookLogoF() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "f",
                color = FacebookBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
