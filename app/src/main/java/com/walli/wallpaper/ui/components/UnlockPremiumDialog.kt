package com.walli.wallpaper.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walli.wallpaper.domain.model.Wallpaper

@Composable
fun UnlockPremiumDialog(
    wallpaper: Wallpaper,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock Premium Wallpaper") },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text("Watch a short video to unlock this premium wallpaper and support our creators.")
            }
        },
        confirmButton = {
            Button(
                onClick = onUnlock,
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isLoading) "Loading Ad..." else "Watch Ad & Unlock")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                // Cancel button stays enabled as per UX requirements
            ) {
                Text("Cancel")
            }
        }
    )
}
