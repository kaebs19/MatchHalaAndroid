package com.chathala.hala.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * حاوية Snackbar موحّدة للتطبيق — استخدمها بدل Toast.
 *
 * مثال:
 * ```
 * val host = rememberHalaSnackbarHost()
 * LaunchedEffect(Unit) {
 *     viewModel.message.collect { host.showSnackbar(it) }
 * }
 * HalaSnackbarHost(host)
 * ```
 */
@Composable
fun rememberHalaSnackbarHost(): SnackbarHostState = remember { SnackbarHostState() }

@Composable
fun HalaSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(hostState = hostState) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}
