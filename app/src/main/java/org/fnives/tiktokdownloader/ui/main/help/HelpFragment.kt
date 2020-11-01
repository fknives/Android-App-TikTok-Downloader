package org.fnives.tiktokdownloader.ui.main.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import org.fnives.tiktokdownloader.R

class HelpFragment : Fragment(R.layout.fragment_help) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val inAppYoutubeView = view.findViewById<YouTubePlayerView>(R.id.in_app_youtube_view)
        val inTikTokYoutubeView = view.findViewById<YouTubePlayerView>(R.id.in_tiktok_youtube_view)
        viewLifecycleOwner.lifecycle.addObserver(inAppYoutubeView)
        viewLifecycleOwner.lifecycle.addObserver(inTikTokYoutubeView)
        val repositoryLinkView = view.findViewById<TextView>(R.id.repository_link_view)
        repositoryLinkView.setOnClickListener {
            startActivity(createRepositoryIntent())
        }
    }

    companion object {

        fun createRepositoryIntent(): Intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fknives/TikTok-Downloader"))

        fun newInstance(): HelpFragment = HelpFragment()
    }
}