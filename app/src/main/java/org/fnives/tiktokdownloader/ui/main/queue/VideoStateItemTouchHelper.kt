package org.fnives.tiktokdownloader.ui.main.queue

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.fnives.tiktokdownloader.data.model.VideoState

class VideoStateItemTouchHelper(
    private val whichItem: (RecyclerView.ViewHolder) -> VideoState?,
    private val onDeleteElement: (VideoState) -> Unit,
    private val onUIMoveElement: (VideoState, VideoState) -> Unit,
    private val onMoveElement: (VideoState, Int) -> Unit
) : ItemTouchHelper.Callback() {

    var dragEnabled: Boolean = true
    var swipeEnabled: Boolean = true
    private var index: Int? = null

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val item = whichItem(viewHolder) ?: return DISABLED_FLAG
        var dragEnabled = dragEnabled
        var swipeEnabled = swipeEnabled
        when (item) {
            is VideoState.InPending -> Unit
            is VideoState.Downloaded -> {
                dragEnabled = false
            }
            is VideoState.InProcess -> {
                dragEnabled = false
                swipeEnabled = false
            }
        }

        val dragFlags = if (dragEnabled) ItemTouchHelper.UP or ItemTouchHelper.DOWN else DISABLED_FLAG
        val swipeFlags = if (swipeEnabled) ItemTouchHelper.START or ItemTouchHelper.END else DISABLED_FLAG
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val dragged = whichItem(target)?.takeIf { it is VideoState.InPending } ?: return false
        val movedTo = whichItem(viewHolder)?.takeIf { it is VideoState.InPending } ?: return false
        onUIMoveElement(dragged, movedTo)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        whichItem(viewHolder)?.let { onDeleteElement(it) }
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        (viewHolder as? MovingItemCallback)?.onMovingStart()
        index = when(actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> viewHolder?.bindingAdapterPosition
            ItemTouchHelper.ACTION_STATE_IDLE -> index
            else -> null
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        (viewHolder as? MovingItemCallback)?.onMovingEnd()
        val videoState = whichItem(viewHolder) ?: return
        val startIndex = index ?: return

        val endIndex = viewHolder.bindingAdapterPosition
        onMoveElement(videoState, endIndex - startIndex)
    }

    companion object {
        private const val DISABLED_FLAG = 0
    }
}