/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray.browser.compose

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.lib.state.ext.observeAsComposableState
import net.waterfox.android.compose.tabstray.TabListItem
import net.waterfox.android.selection.SelectionHolder
import net.waterfox.android.tabstray.TabsTrayState
import net.waterfox.android.tabstray.TabsTrayStore
import net.waterfox.android.tabstray.browser.BrowserTrayInteractor

/**
 * A Compose ViewHolder implementation for "tab" items with list layout.
 *
 * @param interactor [BrowserTrayInteractor] handling tabs interactions in a tab tray.
 * @param tabsTrayStore [TabsTrayStore] containing the complete state of tabs tray and methods to update that.
 * @param selectionHolder [SelectionHolder]<[TabSessionState]> for helping with selecting
 * any number of displayed [TabSessionState]s.
 * @param composeItemView that displays a "tab".
 * @param viewLifecycleOwner [LifecycleOwner] to which this Composable will be tied to.
 */
class ComposeListViewHolder(
    private val interactor: BrowserTrayInteractor,
    private val tabsTrayStore: TabsTrayStore,
    private val selectionHolder: SelectionHolder<TabSessionState>? = null,
    composeItemView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
) : ComposeAbstractTabViewHolder(composeItemView, viewLifecycleOwner) {

    private var delegate: TabsTray.Delegate? = null

    override var tab: TabSessionState? = null
    private val isMultiSelectionSelected = MutableStateFlow(false)
    private val isSelectedTab = MutableStateFlow(false)

    override fun bind(
        tab: TabSessionState,
        isSelected: Boolean,
        styling: TabsTrayStyling,
        delegate: TabsTray.Delegate
    ) {
        this.tab = tab
        this.delegate = delegate
        isSelectedTab.value = isSelected
        bind(tab)
    }

    override fun updateSelectedTabIndicator(showAsSelected: Boolean) {
        isSelectedTab.value = showAsSelected
    }

    override fun showTabIsMultiSelectEnabled(selectedMaskView: View?, isSelected: Boolean) {
        isMultiSelectionSelected.value = isSelected
    }

    private fun onCloseClicked(tab: TabSessionState) {
        delegate?.onTabClosed(tab)
    }

    private fun onClick(tab: TabSessionState) {
        val holder = selectionHolder
        if (holder != null) {
            interactor.onMultiSelectClicked(tab, holder, null)
        } else {
            interactor.onTabSelected(tab)
        }
    }

    private fun onLongClick(tab: TabSessionState) {
        val holder = selectionHolder ?: return
        interactor.onLongClicked(tab, holder)
    }

    @Composable
    override fun Content(tab: TabSessionState) {
        val multiSelectionEnabled = tabsTrayStore.observeAsComposableState {
            state ->
            state.mode is TabsTrayState.Mode.Select
        }.value ?: false
        val isSelectedTabState by isSelectedTab.collectAsState()
        val multiSelectionSelected by isMultiSelectionSelected.collectAsState()

        TabListItem(
            tab = tab,
            isSelected = isSelectedTabState,
            multiSelectionEnabled = multiSelectionEnabled,
            multiSelectionSelected = multiSelectionSelected,
            onCloseClick = ::onCloseClicked,
            onMediaClick = interactor::onMediaClicked,
            onClick = ::onClick,
            onLongClick = ::onLongClick,
        )
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}
