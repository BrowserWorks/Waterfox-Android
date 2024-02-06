/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.history

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.Flow
import mozilla.components.support.base.feature.UserInteractionHandler
import net.waterfox.android.R
import net.waterfox.android.components.components
import net.waterfox.android.theme.WaterfoxTheme

class HistoryComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr), UserInteractionHandler {

    var mode by mutableStateOf<HistoryFragmentState.Mode>(HistoryFragmentState.Mode.Normal)
    var pendingDeletionItems by mutableStateOf<Set<PendingDeletionHistory>>(emptySet())
    var progressIndicatorVisible by mutableStateOf(true)
    var swipeRefreshEnabled by mutableStateOf(true)
    var swipeRefreshRefreshing by mutableStateOf(false)
    var emptyViewVisible by mutableStateOf(true)

    lateinit var interactor: HistoryInteractor
    lateinit var history: Flow<PagingData<History>>
    lateinit var onZeroItemsLoaded: () -> Unit
    lateinit var onEmptyStateChanged: (Boolean) -> Unit

    // A flag to track the empty state of the list. Items are not being deleted immediately,
    // but hidden from the UI until the Undo snackbar will execute the delayed operation.
    // Whether the state has actually zero items or all present items are hidden,
    // the screen should be updated into proper empty/not empty state.
    private var empty = true

    private val itemsWithHeaders: MutableMap<HistoryItemTimeGroup, Int> = mutableMapOf()

    fun updateState(state: HistoryFragmentState) {
        if (state.mode != mode) {
            mode = state.mode
            interactor.onModeSwitched()
        }
        pendingDeletionItems = state.pendingDeletionItems
        progressIndicatorVisible = state.isDeletingItems
        swipeRefreshEnabled =
            state.mode is HistoryFragmentState.Mode.Normal || state.mode is HistoryFragmentState.Mode.Syncing
        swipeRefreshRefreshing = state.mode is HistoryFragmentState.Mode.Syncing
        emptyViewVisible = state.isEmpty
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        WaterfoxTheme {
            Column {
                if (progressIndicatorVisible) {
                    ProgressIndicator()
                }

                val swipeRefreshState =
                    rememberSwipeRefreshState(isRefreshing = swipeRefreshRefreshing)
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { interactor.onRequestSync() },
                    modifier = Modifier.fillMaxSize(),
                    swipeEnabled = swipeRefreshEnabled,
                    indicator = { state, trigger ->
                        SwipeRefreshIndicator(
                            state = state,
                            refreshTriggerDistance = trigger,
                            contentColor = WaterfoxTheme.colors.textPrimary,
                        )
                    },
                ) {
                    val historyList = history.collectAsLazyPagingItems()

                    if (historyList.loadState.source.refresh is LoadState.NotLoading &&
                            historyList.loadState.append.endOfPaginationReached &&
                            historyList.itemCount < 1) {
                        onZeroItemsLoaded()
                    }

                    LazyColumn(
                        modifier = Modifier.semantics {
                            testTagsAsResourceId = true
                            testTag = "history.list"
                        }
                    ) {
                        item {
                            RecentlyClosedNav()
                        }

                        items(
                            count = historyList.itemCount,
                            key = historyList.itemKey { it.id },
                        ) { index ->
                            val history = historyList[index]

                            var timeGroup: HistoryItemTimeGroup? = null
                            var isPendingDeletion = false
                            var groupPendingDeletionCount = 0

                            if (history != null) {
                                if (pendingDeletionItems.isNotEmpty()) {
                                    when (history) {
                                        is History.Regular -> {
                                            isPendingDeletion = pendingDeletionItems.find {
                                                it is PendingDeletionHistory.Item && it.visitedAt == history.visitedAt
                                            } != null
                                        }
                                        is History.Group -> {
                                            isPendingDeletion = pendingDeletionItems.find {
                                                it is PendingDeletionHistory.Group && it.visitedAt == history.visitedAt
                                            } != null

                                            if (!isPendingDeletion) {
                                                groupPendingDeletionCount =
                                                    history.items.count { historyMetadata ->
                                                        pendingDeletionItems.find {
                                                            it is PendingDeletionHistory.MetaData &&
                                                                    it.key == historyMetadata.historyMetadataKey &&
                                                                    it.visitedAt == historyMetadata.visitedAt
                                                        } != null
                                                    }.also {
                                                        if (it == history.items.size) {
                                                            isPendingDeletion = true
                                                        }
                                                    }
                                            }
                                        }
                                        else -> Unit
                                    }
                                }

                                // Add or remove the header and position to the map depending on it's deletion status
                                if (itemsWithHeaders.containsKey(history.historyTimeGroup)) {
                                    if (isPendingDeletion && itemsWithHeaders[history.historyTimeGroup] == index) {
                                        itemsWithHeaders.remove(history.historyTimeGroup)
                                    } else if (isPendingDeletion && itemsWithHeaders[history.historyTimeGroup] != index) {
                                        // do nothing
                                    } else {
                                        if (index <= itemsWithHeaders[history.historyTimeGroup] as Int) {
                                            itemsWithHeaders[history.historyTimeGroup] = index
                                            timeGroup = history.historyTimeGroup
                                        }
                                    }
                                } else if (!isPendingDeletion) {
                                    itemsWithHeaders[history.historyTimeGroup] = index
                                    timeGroup = history.historyTimeGroup
                                }

                                if (empty && !isPendingDeletion) {
                                    empty = false
                                    onEmptyStateChanged(false)
                                } else {//if (...) {
                                    // If we reached the bottom of the list and there still has been zero visible items,
                                    // we can can change the History view state to empty.
                                    if (empty) {
                                        onEmptyStateChanged(true)
                                    }
                                }

                                HistoryListItem(
                                    item = history,
                                    timeGroup = timeGroup,
                                    mode = mode,
                                    isPendingDeletion = isPendingDeletion,
                                    groupPendingDeletionCount = groupPendingDeletionCount,
                                    holder = mode,
                                    interactor = interactor,
                                )
                            }
                        }
                    }

                    if (emptyViewVisible) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyView()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RecentlyClosedNav() {
        val enabled = mode == HistoryFragmentState.Mode.Normal
        val numRecentTabs = components.core.store.state.closedTabs.size
        val numRecentTabsText = stringResource(
            if (numRecentTabs == 1) {
                R.string.recently_closed_tab
            } else {
                R.string.recently_closed_tabs
            },
            numRecentTabs,
        )

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .defaultMinSize(minHeight = 56.dp)
                .alpha(if (enabled) 1f else 0.7f)
                .clickable(
                    enabled = enabled,
                    onClick = { interactor.onRecentlyClosedClicked() },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(16.dp))

            Image(
                painter = painterResource(R.drawable.ic_multiple_tabs),
                contentDescription = null,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 32.dp, end = 16.dp),
            ) {
                Text(
                    stringResource(R.string.library_recently_closed_tabs),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = TextStyle(color = WaterfoxTheme.colors.textPrimary),
                )

                Text(
                    numRecentTabsText,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = TextStyle(color = WaterfoxTheme.colors.textSecondary),
                )
            }
        }
    }

    @Composable
    private fun EmptyView() {
        Text(
            stringResource(R.string.history_empty_message),
            color = WaterfoxTheme.colors.textSecondary,
            fontSize = 16.sp,
        )
    }

    @Composable
    private fun ProgressIndicator() {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .offset(y = (-3).dp),
        )
    }

    override fun onBackPressed(): Boolean {
        return interactor.onBackPressed()
    }

}
