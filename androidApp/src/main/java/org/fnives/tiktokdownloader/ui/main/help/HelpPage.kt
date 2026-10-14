package org.fnives.tiktokdownloader.ui.main.help

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import org.fnives.tiktokdownloader.R

@Composable
fun HelpPage() {
    Column(
        Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HelpCard {
            Text(
                stringResource(R.string.how_to_use_in_app),
                style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.how_to_use_in_app_description))
            Youtube("NXv3JpmwA8Y")
        }
        HelpCard {
            Text(
                stringResource(R.string.how_to_use_from_tiktok),
                style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.how_to_use_from_tiktok_description))
            Youtube("jxaxffE8c4c")
        }
        HelpCard {
            Text(
                stringResource(R.string.additional_information),
                style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.error_handling))
            Text(stringResource(R.string.captcha_handling))
            val activity = LocalActivity.current
            Text(stringResource(R.string.link_to_repository), Modifier.clickable(onClick = {
                activity?.startActivity(getHelpIntent())
            }))
        }
    }
}

private fun getHelpIntent() = Intent(
    Intent.ACTION_VIEW,
    "https://github.com/fknives/TikTok-Downloader".toUri()
)

@Composable
private fun HelpCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun Youtube(videoId: String) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(factory = { context ->
        val view = YouTubePlayerView(context)

        view.enableAutomaticInitialization = false
        view.initialize(
            object : AbstractYouTubePlayerListener() {},
            handleNetworkEvents = true,
            playerOptions = IFramePlayerOptions.getDefault(context),
            videoId = videoId
        )

        lifecycleOwner.lifecycle.addObserver(view)
        return@AndroidView view
    })
}