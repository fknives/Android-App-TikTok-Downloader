package org.fnives.tiktokdownloader.ui.main.queue

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
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
            startActivity(intent)
        }
    }

    private fun recyclerViewSetup(recycler: RecyclerView) {
        val adapter = QueueItemAdapter(
            itemClicked = viewModel::onItemClicked,
            urlClicked = viewModel::onUrlClicked
        )
        recycler.adapter = adapter

        val touchHelper = ItemTouchHelper(PendingItemTouchHelper(
            whichItem = { adapter.currentList.getOrNull(it.bindingAdapterPosition) },
            onDeleteElement = viewModel::onElementDeleted,
            onMovedElement = viewModel::onElementMoved
        ))
        touchHelper.attachToRecyclerView(recycler)

        viewModel.downloads.observe(viewLifecycleOwner) { videoStates ->
            adapter.submitList(videoStates, Runnable {
                val indexToScrollTo = videoStates.indexOfFirst { it is VideoState.InProcess }
                    .takeIf { it != -1 } ?: return@Runnable
                recycler.smoothScrollToPosition(indexToScrollTo)
            })
        }
    }

    class PendingItemTouchHelper(
        private val whichItem: (RecyclerView.ViewHolder) -> VideoState?,
        private val onDeleteElement: (VideoState) -> Unit,
        private val onMovedElement: (VideoState, VideoState) -> Boolean
    ) : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val item = whichItem(viewHolder) ?: return 0
            when (item) {
                is VideoState.InPending -> Unit
                is VideoState.Downloaded,
                is VideoState.InProcess -> return 0
            }

            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
            return makeMovementFlags(dragFlags, swipeFlags)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val dragged = whichItem(target) ?: return false
            val movedTo = whichItem(viewHolder) ?: return false
            return onMovedElement(dragged, movedTo)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            whichItem(viewHolder)?.let { onDeleteElement(it) }
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