package com.chathala.hala.feature.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chathala.hala.ui.components.SkeletonBlock

/**
 * Skeleton يحاكي شكل ProfileScreen — يعرض أثناء انتظار بيانات المستخدم.
 */
@Composable
fun ProfileSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // Avatar
        SkeletonBlock(
            modifier = Modifier.size(140.dp),
            shape = CircleShape
        )
        Spacer(Modifier.height(14.dp))

        // Name
        SkeletonBlock(
            modifier = Modifier.width(160.dp).height(28.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Pills row
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBlock(
                modifier = Modifier.width(110.dp).height(32.dp),
                shape = RoundedCornerShape(50)
            )
            SkeletonBlock(
                modifier = Modifier.width(80.dp).height(32.dp),
                shape = RoundedCornerShape(50)
            )
        }
        Spacer(Modifier.height(24.dp))

        // 3 cards placeholders
        repeat(3) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = MaterialTheme.shapes.large
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}
