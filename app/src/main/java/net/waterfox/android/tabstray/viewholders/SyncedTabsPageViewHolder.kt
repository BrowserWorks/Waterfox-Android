/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray.viewholders

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.lib.state.ext.observeAsComposableState
import net.waterfox.android.ext.settings
import net.waterfox.android.tabstray.NavigationInteractor
import net.waterfox.android.tabstray.TabsTrayState
import net.waterfox.android.tabstray.TabsTrayStore
import net.waterfox.android.tabstray.syncedtabs.SyncedTabsList
import net.waterfox.android.theme.WaterfoxTheme
import net.waterfox.android.theme.Theme

/**
 * Temporary ViewHolder to render [SyncedTabsList] until all of the Tabs Tray is written in Compose.
 *
 * @param composeView Root ComposeView passed-in from TrayPagerAdapter.
 * @param tabsTrayStore Store used as a Composable State to listen for changes to [TabsTrayState.syncedTabs].
 * @param navigationInteractor The lambda for handling clicks on synced tabs.
 */
class SyncedTabsPageViewHolder(
    private val composeView: ComposeView,
    private val tabsTrayStore: TabsTrayStore,
    private val navigationInteractor: NavigationInteractor,
) : AbstractPageViewHolder(composeView) {

    fun bind() {
        composeView.setContent {
            val tabs = tabsTrayStore.observeAsComposableState { state -> state.syncedTabs }.value
            WaterfoxTheme(theme = Theme.getTheme(allowPrivateTheme = false)) {
                SyncedTabsList(
                    syncedTabs = tabs ?: emptyList(),
                    taskContinuityEnabled = composeView.context.settings().enableTaskContinuityEnhancements,
                    onTabClick = navigationInteractor::onSyncedTabClicked,
                )
            }
        }
    }

    override fun bind(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) = Unit // no-op

    override fun detachedFromWindow() = Unit // no-op

    override fun attachedToWindow() = Unit // no-op

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}
