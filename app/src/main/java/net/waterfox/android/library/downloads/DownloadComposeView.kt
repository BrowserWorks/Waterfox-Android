/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.downloads

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import mozilla.components.feature.downloads.toMegabyteOrKilobyteString
import mozilla.components.support.base.feature.UserInteractionHandler
import net.waterfox.android.R
import net.waterfox.android.ext.getIcon
import net.waterfox.android.library.LibrarySiteItem
import net.waterfox.android.selection.SelectionInteractor
import net.waterfox.android.theme.WaterfoxTheme

/**
 * Interface for the DownloadViewInteractor. This interface is implemented by objects that want
 * to respond to user interaction on the DownloadView
 */
interface DownloadViewInteractor : SelectionInteractor<DownloadItem> {

    /**
     * Called on backpressed to exit edit mode
     */
    fun onBackPressed(): Boolean

    /**
     * Called when the mode is switched so we can invalidate the menu
     */
    fun onModeSwitched()

    /**
     * Called when multiple downloads items are deleted
     * @param items the downloads items to delete
     */
    fun onDeleteSome(items: Set<DownloadItem>)
}

class DownloadComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr), UserInteractionHandler {

    var downloadList by mutableStateOf<List<DownloadItem>>(emptyList())
    var mode by mutableStateOf<DownloadFragmentState.Mode>(DownloadFragmentState.Mode.Normal)
    var pendingDeletionIds by mutableStateOf<Set<String>>(emptySet())
    var progressIndicatorVisible by mutableStateOf(true)
    var emptyViewVisible by mutableStateOf(false)

    lateinit var interactor: DownloadInteractor

    fun updateState(state: DownloadFragmentState) {
        val oldMode = mode

        downloadList = state.items
        mode = state.mode
        pendingDeletionIds = state.pendingDeletionIds
        progressIndicatorVisible = state.isDeletingItems
        emptyViewVisible = state.pendingDeletionIds.size == state.items.size

        if (state.mode::class != oldMode::class) {
            interactor.onModeSwitched()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        WaterfoxTheme {
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = false)
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {},
                modifier = Modifier.fillMaxSize(),
                swipeEnabled = false,
            ) {
                LazyColumn(
                    modifier = Modifier.semantics {
                        testTagsAsResourceId = true
                        testTag = "download.list"
                    },
                    contentPadding = PaddingValues(top = 8.dp),
                ) {
                    items(
                        items = downloadList,
                        key = { download -> download.id },
                    ) { download ->
                        val shouldHideMenu = mode is DownloadFragmentState.Mode.Editing
                        val menuIcon = if (shouldHideMenu) null else R.drawable.ic_delete
                        var onMenuClick: ((DownloadItem) -> Unit)? =
                            { interactor.onDeleteSome(setOf(download)) }
                        onMenuClick = if (shouldHideMenu) null else onMenuClick

                        if (!pendingDeletionIds.contains(download.id)) {
                            LibrarySiteItem(
                                favicon = download.getIcon(),
                                title = download.fileName,
                                url = download.size.toLong().toMegabyteOrKilobyteString(),
                                selected = download in mode.selectedItems,
                                menuIcon = menuIcon,
                                menuContentDescription = stringResource(R.string.download_delete_item_1),
                                onMenuClick = onMenuClick,
                                item = download,
                                holder = mode,
                                interactor = interactor,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (emptyViewVisible) {
                        EmptyView()
                    }
                }

                if (progressIndicatorVisible) {
                    ProgressIndicator()
                }
            }
        }
    }

    @Composable
    private fun EmptyView() {
        Text(
            stringResource(R.string.download_empty_message_1),
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

    override fun onBackPressed() = interactor.onBackPressed()

}
