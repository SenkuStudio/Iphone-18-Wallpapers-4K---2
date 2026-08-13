import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.walli.spidermanwallpaper.domain.model.Wallpaper

@Composable
fun UnlockPremiumDialog(
    wallpaper: Wallpaper,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = if (isLoading) ({}) else onDismiss,
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
            if (!isLoading) {
                Button(onClick = onUnlock) {
                    Text("Watch Ad & Unlock")
                }
            }
        },
        dismissButton = {
            if (!isLoading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
