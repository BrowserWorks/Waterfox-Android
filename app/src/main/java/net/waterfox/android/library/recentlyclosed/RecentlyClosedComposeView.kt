/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.recentlyclosed

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import mozilla.components.browser.state.state.recover.TabState
import net.waterfox.android.R
import net.waterfox.android.library.LibrarySiteItem
import net.waterfox.android.selection.SelectionHolder
import net.waterfox.android.selection.SelectionInteractor
import net.waterfox.android.theme.WaterfoxTheme

interface RecentlyClosedInteractor : SelectionInteractor<TabState> {
    /**
     * Called when the view more history option is tapped.
     */
    fun onNavigateToHistory()

    /**
     * Called when recently closed tab is selected for deletion.
     *
     * @param tab the recently closed tab to delete.
     */
    fun onDelete(tab: TabState)
}

class RecentlyClosedComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr), SelectionHolder<TabState> {

    var recentlyClosedList by mutableStateOf<List<TabState>>(emptyList())
    var selectedTabs by mutableStateOf<Set<TabState>>(emptySet())
    
    lateinit var interactor: RecentlyClosedFragmentInteractor
    
    fun updateState(state: RecentlyClosedFragmentState) {
        recentlyClosedList = state.items
        selectedTabs = state.selectedTabs
    }

    override val selectedItems: Set<TabState>
        get() = selectedTabs

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        WaterfoxTheme {
            LazyColumn(
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                    testTag = "recently.closed.list"
                }
            ) {
                item {
                    ViewMoreHistoryView()
                }

                items(
                    items = recentlyClosedList,
                    key = { tab -> tab.id },
                ) { tab ->
                    LibrarySiteItem(
                        title = tab.title.ifEmpty { tab.url },
                        url = tab.url,
                        selected = tab in selectedTabs,
                        menuIcon = R.drawable.ic_close,
                        menuContentDescription = stringResource(R.string.history_delete_item),
                        onMenuClick = { interactor.onDelete(tab) },
                        item = tab,
                        holder = this@RecentlyClosedComposeView,
                        interactor = interactor,
                    )
                }
            }

            if (recentlyClosedList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyView()
                }
            }
        }
    }

    @Composable
    private fun ViewMoreHistoryView() {
        LibrarySiteItem(
            favicon = R.drawable.ic_history,
            title = stringResource(R.string.recently_closed_show_full_history),
            item = null,
            onItemClick = { interactor.onNavigateToHistory() }
        )
    }

    @Composable
    private fun EmptyView() {
        Text(
            stringResource(R.string.recently_closed_empty_message),
            color = WaterfoxTheme.colors.textSecondary,
            fontSize = 16.sp,
        )
    }

}
