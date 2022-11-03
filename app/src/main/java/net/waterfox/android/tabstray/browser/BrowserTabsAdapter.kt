/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray.browser

import android.content.Context
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.SelectableTabViewHolder
import mozilla.components.browser.tabstray.TabsAdapter.Companion.PAYLOAD_DONT_HIGHLIGHT_SELECTED_ITEM
import mozilla.components.browser.tabstray.TabsAdapter.Companion.PAYLOAD_HIGHLIGHT_SELECTED_ITEM
import mozilla.components.browser.thumbnails.loader.ThumbnailLoader
import net.waterfox.android.components.Components
import net.waterfox.android.ext.components
import net.waterfox.android.selection.SelectionHolder
import net.waterfox.android.tabstray.TabsTrayStore
import net.waterfox.android.tabstray.browser.compose.ComposeGridViewHolder
import net.waterfox.android.tabstray.browser.compose.ComposeListViewHolder

/**
 * A [RecyclerView.Adapter] for browser tabs.
 *
 * @param context [Context] used for various platform interactions or accessing [Components]
 * @param interactor [BrowserTrayInteractor] handling tabs interactions in a tab tray.
 * @param store [TabsTrayStore] containing the complete state of tabs tray and methods to update that.
 * @param viewLifecycleOwner [LifecycleOwner] life cycle owner for the view.
 */
class BrowserTabsAdapter(
    private val context: Context,
    val interactor: BrowserTrayInteractor,
    private val store: TabsTrayStore,
    internal val viewLifecycleOwner: LifecycleOwner
) : TabsAdapter<SelectableTabViewHolder>(interactor) {

    /**
     * The layout types for the tabs.
     */
    enum class ViewType(val layoutRes: Int) {
        LIST(ComposeListViewHolder.LAYOUT_ID),
        GRID(ComposeGridViewHolder.LAYOUT_ID)
    }

    /**
     * Tracks the selected tabs in multi-select mode.
     */
    var selectionHolder: SelectionHolder<TabSessionState>? = null

    private val selectedItemAdapterBinding = SelectedItemAdapterBinding(store, this)
    private val imageLoader = ThumbnailLoader(context.components.core.thumbnailStorage)

    override fun getItemViewType(position: Int) =
        if (context.components.settings.gridTabView) ViewType.GRID.layoutRes
        else ViewType.LIST.layoutRes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        if (viewType == ViewType.LIST.layoutRes) ComposeListViewHolder(
            interactor = interactor,
            tabsTrayStore = store,
            selectionHolder = selectionHolder,
            composeItemView = ComposeView(parent.context),
            viewLifecycleOwner = viewLifecycleOwner
        )
        else ComposeGridViewHolder(
            interactor = interactor,
            store = store,
            selectionHolder = selectionHolder,
            composeItemView = ComposeView(parent.context),
            viewLifecycleOwner = viewLifecycleOwner
        )

    override fun onBindViewHolder(holder: SelectableTabViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.tab?.let { tab ->
            selectionHolder?.let {
                holder.showTabIsMultiSelectEnabled(
                    null,
                    (it.selectedItems.map { item -> item.id }).contains(tab.id)
                )
            }
        }
    }

    /**
     * Over-ridden [onBindViewHolder] that uses the payloads to notify the selected tab how to
     * display itself.
     */
    override fun onBindViewHolder(holder: SelectableTabViewHolder, position: Int, payloads: List<Any>) {
        if (currentList.isEmpty()) return

        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        val tab = getItem(position)
        if (tab.id == selectedTabId) {
            if (payloads.contains(PAYLOAD_HIGHLIGHT_SELECTED_ITEM)) {
                holder.updateSelectedTabIndicator(true)
            } else if (payloads.contains(PAYLOAD_DONT_HIGHLIGHT_SELECTED_ITEM)) {
                holder.updateSelectedTabIndicator(false)
            }
        }

        selectionHolder?.let {
            holder.showTabIsMultiSelectEnabled(
                null,
                it.selectedItems.map { item -> item.id }.contains(tab.id)
            )
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        selectedItemAdapterBinding.start()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        selectedItemAdapterBinding.stop()
    }
}
