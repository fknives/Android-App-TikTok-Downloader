package org.fnives.tiktokdownloader.ui.main.queue

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.data.model.VideoState
import org.fnives.tiktokdownloader.di.provideViewModels

class QueueFragment : Fragment(R.layout.fragment_queue) {

    private val viewModel by provideViewModels<QueueViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        recyclerViewSetup(recycler)
        navigationSetup()

        val saveUrlCta = view.findViewById<Button>(R.id.save_cta)
        val input = view.findViewById<EditText>(R.id.download_url_input)
        input.doAfterTextChanged {
            saveUrlCta.isEnabled = it?.isNotBlank() == true
        }
        saveUrlCta.setOnClickListener {
            viewModel.onSaveClicked(input.text?.toString().orEmpty())
            input.setText("")
        }
    }

    private fun navigationSetup() {
        viewModel.navigationEvent.observe(viewLifecycleOwner) {
            val intent = when (val data = it.item) {
                is QueueViewModel.NavigationEvent.OpenBrowser -> {
                    createBrowserIntent(data.url)
                }
                is QueueViewModel.NavigationEvent.OpenGallery ->
                    createGalleryIntent(data.uri)
                null -> return@observe
            }
            try {
                startActivity(intent)
            } catch(activityNotFoundException: ActivityNotFoundException) {
                val anchor = activity?.findViewById<CoordinatorLayout>(R.id.snack_bar_anchor) ?: return@observe
                Snackbar.make(anchor, R.string.could_not_open, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun recyclerViewSetup(recycler: RecyclerView) {
        val adapter = QueueItemAdapter(
            itemClicked = viewModel::onItemClicked,
            urlClicked = viewModel::onUrlClicked
        )
        recycler.adapter = adapter

        val callback = VideoStateItemTouchHelper(
            whichItem = { adapter.currentList.getOrNull(it.bindingAdapterPosition) },
            onDeleteElement = viewModel::onElementDeleted,
            onUIMoveElement = adapter::swap,
            onMoveElement = viewModel::onElementMoved
        )
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recycler)

        viewModel.downloads.observe(viewLifecycleOwner) { videoStates ->
            callback.dragEnabled = videoStates.none { it is VideoState.InProcess }

            adapter.submitList(videoStates, Runnable {
                val indexToScrollTo = videoStates.indexOfFirst { it is VideoState.InProcess }
                    .takeIf { it != -1 } ?: return@Runnable
                recycler.smoothScrollToPosition(indexToScrollTo)
            })
        }
    }

    companion object {

        fun newInstance(): QueueFragment = QueueFragment()

        fun createBrowserIntent(url: String): Intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url))

        fun createGalleryIntent(uri: String): Intent =
            Intent(Intent.ACTION_VIEW, uri.toUri())
    }
}