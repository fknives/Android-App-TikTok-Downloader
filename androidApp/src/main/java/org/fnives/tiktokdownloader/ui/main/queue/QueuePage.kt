package org.fnives.tiktokdownloader.ui.main.queue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.data.model.VideoState
import org.fnives.tiktokdownloader.di.ServiceLocator
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun QueuePage(
    viewModel: QueueViewModel = ServiceLocator.viewModel(),
) {
    val state = rememberCollapsableInputHeaderState()
    QueueNavigationSideEffects(viewModel)

    Column(
        Modifier
            .fillMaxSize()
            .nestedScroll(state.nestedScrollConnection)
    ) {
        var input by remember { mutableStateOf("") }
        CollapsableInputHeader(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            notice = { Text(stringResource(R.string.copy_the_link_from_tiktok_here_to_download_it)) },
            input = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = {
                        Text(stringResource(R.string.tik_tok_link))
                    }
                )
            },
            action = {
                OutlinedButton(
                    enabled = input.isNotBlank(),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        width = if (input.isNotBlank()) 2.dp else 1.dp,
                        color = if (input.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f)
                        },
                    ),
                    onClick = {
                        viewModel.onSaveClicked(input)
                        input = ""
                    }) { Text(stringResource(R.string.start)) }
            },
            state = state,
        )

        val viewStates by viewModel.downloads.collectAsStateWithLifecycle()
        var orderedItems by remember(viewStates) {
            mutableStateOf(viewStates)
        }
        val lazyListState = rememberLazyListState()
        val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
            orderedItems =
                orderedItems.toMutableList().apply { add(to.index, removeAt(from.index)) }
            viewModel.onElementMoved(viewModel.downloads.value[from.index], to.index - from.index)
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = lazyListState
        ) {
            items(
                orderedItems,
                { it.id }
            ) { item ->
                ReorderableItem(reorderableLazyListState, key = item.id) {
                    val dismissState = rememberSwipeToDismissBoxState()
                    SwipeToDismissBox(
                        dismissState,
                        backgroundContent = {},
                        onDismiss = {
                            viewModel.onElementDeleted(item)
                        }
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        QueuedItem(
                            item,
                            dragModifier = Modifier.longPressDraggableHandle(
                                interactionSource = interactionSource,
                            ),
                            interactionSource = interactionSource,
                            onUrlClick = {
                                viewModel.onUrlClicked(item.url)
                            },
                            onItemClick = {
                                if (item is VideoState.Downloaded) {
                                    viewModel.onItemClicked(item.videoDownloaded.uri)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueuedItem(
    model: VideoState,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onItemClick: () -> Unit,
    onUrlClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 0.dp,
            hoveredElevation = 2.dp,
            draggedElevation = 8.dp,
            pressedElevation = 4.dp,
            focusedElevation = 4.dp
        ),
        interactionSource = interactionSource,
        onClick = onItemClick,
    ) {
        Row(
            Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val imageSize = 64.dp
            when (model) {
                is VideoState.Downloaded -> AsyncImage(
                    model.videoDownloaded.uri,
                    contentDescription = null,
                    modifier = Modifier.size(imageSize)
                )

                is VideoState.InPending,
                is VideoState.InProcess -> Image(
                    painterResource(R.drawable.ic_twotone_image),
                    contentDescription = null,
                    modifier = Modifier.size(imageSize)
                )
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    model.url,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onUrlClick)
                )
                when (model) {
                    is VideoState.Downloaded -> Text(
                        stringResource(R.string.status_finished),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    is VideoState.InPending -> Text(
                        stringResource(R.string.status_pending),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    is VideoState.InProcess -> LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (model !is VideoState.Downloaded) {
                IconButton(
                    modifier = dragModifier.align(Alignment.CenterVertically),
                    onClick = {},
                ) {
                    Icon(painterResource(R.drawable.ic_drag_handle), contentDescription = "Reorder")
                }
            }
        }
    }
}
