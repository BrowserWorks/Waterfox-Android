/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.store.BrowserStore
import net.waterfox.android.components.AppStore
import net.waterfox.android.tabstray.browser.BrowserTabsAdapter
import net.waterfox.android.tabstray.browser.BrowserTrayInteractor
import net.waterfox.android.tabstray.browser.InactiveTabsAdapter
import net.waterfox.android.tabstray.browser.InactiveTabsInteractor
import net.waterfox.android.tabstray.viewholders.AbstractPageViewHolder
import net.waterfox.android.tabstray.viewholders.NormalBrowserPageViewHolder
import net.waterfox.android.tabstray.viewholders.PrivateBrowserPageViewHolder
import net.waterfox.android.tabstray.viewholders.SyncedTabsPageViewHolder

@Suppress("LongParameterList")
class TrayPagerAdapter(
    @get:VisibleForTesting internal val context: Context,
    @get:VisibleForTesting internal val lifecycleOwner: LifecycleOwner,
    @get:VisibleForTesting internal val tabsTrayStore: TabsTrayStore,
    @get:VisibleForTesting internal val browserInteractor: BrowserTrayInteractor,
    @get:VisibleForTesting internal val navInteractor: NavigationInteractor,
    @get:VisibleForTesting internal val tabsTrayInteractor: TabsTrayInteractor,
    @get:VisibleForTesting internal val browserStore: BrowserStore,
    @get:VisibleForTesting internal val appStore: AppStore,
    @get:VisibleForTesting internal val inactiveTabsInteractor: InactiveTabsInteractor,
) : RecyclerView.Adapter<AbstractPageViewHolder>() {

    /**
     * ⚠️ N.B: Scrolling to the selected tab depends on the order of these adapters. If you change
     * the ordering or add/remove an adapter, please update [NormalBrowserPageViewHolder.scrollToTab] and
     * the layout manager.
     */
    private val normalAdapter by lazy {
        ConcatAdapter(
            InactiveTabsAdapter(
                lifecycleOwner = lifecycleOwner,
                tabsTrayStore = tabsTrayStore,
                inactiveTabsInteractor = inactiveTabsInteractor
            ),
            BrowserTabsAdapter(context, browserInteractor, tabsTrayStore, lifecycleOwner)
        )
    }

    private val privateAdapter by lazy {
        BrowserTabsAdapter(
            context,
            browserInteractor,
            tabsTrayStore,
            lifecycleOwner
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractPageViewHolder =
        when (viewType) {
            NormalBrowserPageViewHolder.LAYOUT_ID -> {
                NormalBrowserPageViewHolder(
                    LayoutInflater.from(parent.context).inflate(viewType, parent, false),
                    lifecycleOwner,
                    tabsTrayStore,
                    browserStore,
                    appStore,
                    tabsTrayInteractor
                )
            }
            PrivateBrowserPageViewHolder.LAYOUT_ID -> {
                PrivateBrowserPageViewHolder(
                    LayoutInflater.from(parent.context).inflate(viewType, parent, false),
                    tabsTrayStore,
                    browserStore,
                    tabsTrayInteractor
                )
            }
            SyncedTabsPageViewHolder.LAYOUT_ID -> {
                SyncedTabsPageViewHolder(
                    composeView = ComposeView(parent.context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    },
                    tabsTrayStore = tabsTrayStore,
                    navigationInteractor = navInteractor
                )
            }
            else -> throw IllegalStateException("Unknown viewType.")
        }

    /**
     * Until [TrayPagerAdapter] is replaced with a Compose implementation, [SyncedTabsPageViewHolder]
     * will need to be called with an empty bind() function since it no longer needs an adapter to render.
     * For more details: https://github.com/mozilla-mobile/fenix/issues/21318
     */
    override fun onBindViewHolder(viewHolder: AbstractPageViewHolder, position: Int) {
        when (viewHolder) {
            is NormalBrowserPageViewHolder -> viewHolder.bind(normalAdapter)
            is PrivateBrowserPageViewHolder -> viewHolder.bind(privateAdapter)
            is SyncedTabsPageViewHolder -> viewHolder.bind()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            POSITION_NORMAL_TABS -> NormalBrowserPageViewHolder.LAYOUT_ID
            POSITION_PRIVATE_TABS -> PrivateBrowserPageViewHolder.LAYOUT_ID
            POSITION_SYNCED_TABS -> SyncedTabsPageViewHolder.LAYOUT_ID
            else -> throw IllegalStateException("Unknown position.")
        }
    }

    override fun onViewAttachedToWindow(holder: AbstractPageViewHolder) {
        holder.attachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: AbstractPageViewHolder) {
        holder.detachedFromWindow()
    }

    override fun getItemCount(): Int = TRAY_TABS_COUNT

    companion object {
        const val TRAY_TABS_COUNT = 3

        val POSITION_NORMAL_TABS = Page.NormalTabs.ordinal
        val POSITION_PRIVATE_TABS = Page.PrivateTabs.ordinal
        val POSITION_SYNCED_TABS = Page.SyncedTabs.ordinal
    }
}
