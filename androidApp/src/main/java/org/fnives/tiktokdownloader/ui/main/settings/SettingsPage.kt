package org.fnives.tiktokdownloader.ui.main.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.di.ServiceLocator
import org.fnives.tiktokdownloader.errortracking.SendErrorsAsEmail
import org.fnives.tiktokdownloader.ui.permission.Permission
import org.fnives.tiktokdownloader.ui.permission.rememberPermissionRequester

@Composable
fun SettingsPage(
    viewModel: SettingsViewModel = ServiceLocator.viewModel()
) {
    val permissionRequester = rememberPermissionRequester()
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.settings), style =
                MaterialTheme.typography.headlineSmall
        )
        Card(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier
                        .clickable {
                            val newValue = !userPreferences.alwaysOpenApp
                            viewModel.setAlwaysOpenApp(newValue)
                            if (!newValue) {
                                permissionRequester.request(Permission.NOTIFICATION)
                            }
                        }
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.user_preference_always_open_app),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = userPreferences.alwaysOpenApp,
                        onCheckedChange = { checked ->
                            viewModel.setAlwaysOpenApp(checked)
                            if (!checked) {
                                permissionRequester.request(Permission.NOTIFICATION)
                            }
                        }
                    )
                }
                val context = LocalContext.current
                val showReportButton by viewModel.showReportButton.collectAsStateWithLifecycle()
                if (showReportButton) {
                    Text(
                        stringResource(R.string.report_errors),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable {
                                SendErrorsAsEmail.send(context)
                            }
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .padding(horizontal = 16.dp)

                    )
                }
            }
        }
    }
}