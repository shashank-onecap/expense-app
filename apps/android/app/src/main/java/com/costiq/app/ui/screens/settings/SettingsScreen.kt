package com.costiq.app.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.costiq.app.BuildConfig
import com.costiq.app.ui.components.CostiqCard
import com.costiq.app.ui.components.iconFor
import com.costiq.app.ui.theme.CostiqTheme
import com.costiq.app.ui.theme.Ink
import com.costiq.app.ui.theme.Paper
import com.costiq.app.ui.theme.TextBody
import com.costiq.app.ui.theme.Vermilion

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val captureEnabled by viewModel.smsCaptureEnabled.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        hasPermission = results[Manifest.permission.RECEIVE_SMS] == true
        // Turning capture on only makes sense once the OS grant actually came through.
        if (hasPermission) viewModel.setSmsCaptureEnabled(true)
    }

    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CostiqTheme.extendedColors.card)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(iconFor("arrow-left"), contentDescription = "Back", tint = TextBody, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.weight(1f))
        }

        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CostiqCard {
                Column {
                    Text(
                        "READING BANK MESSAGES",
                        style = MaterialTheme.typography.labelSmall,
                        color = CostiqTheme.extendedColors.textMuted,
                    )
                    Spacer(Modifier.height(9.dp))

                    if (!hasPermission) {
                        Text(
                            "Costiq can't read your messages yet — you chose to add expenses by hand instead.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CostiqTheme.extendedColors.textMuted,
                        )
                        Spacer(Modifier.height(13.dp))
                        Button(
                            onClick = {
                                val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissions += Manifest.permission.POST_NOTIFICATIONS
                                }
                                permissionLauncher.launch(permissions.toTypedArray())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Vermilion, contentColor = Color.White),
                        ) {
                            Text("Allow message reading")
                        }
                    } else {
                        SettingsToggleRow(
                            label = "Read bank messages",
                            iconName = "message-square-lock",
                            checked = captureEnabled,
                            onToggle = { viewModel.setSmsCaptureEnabled(!captureEnabled) },
                        )
                        Text(
                            if (captureEnabled) {
                                "New expenses are captured from bank SMS automatically. Turn this off any time — existing expenses stay, new messages just stop being read."
                            } else {
                                "New messages aren't being read right now. Turn this back on whenever you want, or keep adding expenses by hand."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = CostiqTheme.extendedColors.textMuted,
                        )
                    }
                }
            }

            CostiqCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("${BuildConfig.WEB_BASE_URL.trimEnd('/')}/dashboard") },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(iconFor("layout-dashboard"), contentDescription = null, tint = CostiqTheme.extendedColors.textMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(13.dp))
                    Text("Open web dashboard", style = MaterialTheme.typography.bodyMedium, color = Ink)
                }
            }

            CostiqCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = viewModel::signOut),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(iconFor("log-out"), contentDescription = null, tint = Vermilion, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(13.dp))
                    Text("Sign out", style = MaterialTheme.typography.bodyMedium, color = Vermilion)
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(label: String, iconName: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp)
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(iconFor(iconName), contentDescription = null, tint = CostiqTheme.extendedColors.textMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(13.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 23.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (checked) Vermilion else CostiqTheme.extendedColors.borderMedium)
                .padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}
