/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.components.appstate

import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.crash.Crash.NativeCodeCrash
import mozilla.components.lib.state.State
import net.waterfox.android.home.HomeFragment
import net.waterfox.android.home.Mode
import net.waterfox.android.home.recentbookmarks.RecentBookmark
import net.waterfox.android.home.recentsyncedtabs.RecentSyncedTabState
import net.waterfox.android.home.recenttabs.RecentTab
import net.waterfox.android.home.recentvisits.RecentlyVisitedItem
import net.waterfox.android.library.history.PendingDeletionHistory
import net.waterfox.android.wallpapers.WallpaperState

/**
 * Value type that represents the state of the tabs tray.
 *
 * @property inactiveTabsExpanded A flag to know if the Inactive Tabs section of the Tabs Tray
 * should be expanded when the tray is opened.
 * @property firstFrameDrawn Flag indicating whether the first frame of the homescreen has been drawn.
 * @property nonFatalCrashes List of non-fatal crashes that allow the app to continue being used.
 * @property collections The list of [TabCollection] to display in the [HomeFragment].
 * @property expandedCollections A set containing the ids of the [TabCollection] that are expanded
 *                               in the [HomeFragment].
 * @property mode The state of the [HomeFragment] UI.
 * @property topSites The list of [TopSite] in the [HomeFragment].
 * @property showCollectionPlaceholder If true, shows a placeholder when there are no collections.
 * @property recentTabs The list of recent [RecentTab] in the [HomeFragment].
 * @property recentSyncedTabState The [RecentSyncedTabState] in the [HomeFragment].
 * @property recentBookmarks The list of recently saved [BookmarkNode]s to show on the [HomeFragment].
 * @property recentHistory The list of [RecentlyVisitedItem]s.
 * @property messaging State related messages.
 * @property pendingDeletionHistoryItems The set of History items marked for removal in the UI,
 * awaiting to be removed once the Undo snackbar hides away.
 * Also serves as an in memory cache of all stories mapped by category allowing for quick stories filtering.
 */
data class AppState(
    val inactiveTabsExpanded: Boolean = false,
    val firstFrameDrawn: Boolean = false,
    val nonFatalCrashes: List<NativeCodeCrash> = emptyList(),
    val collections: List<TabCollection> = emptyList(),
    val expandedCollections: Set<Long> = emptySet(),
    val mode: Mode = Mode.Normal,
    val topSites: List<TopSite> = emptyList(),
    val showCollectionPlaceholder: Boolean = false,
    val recentTabs: List<RecentTab> = emptyList(),
    val recentSyncedTabState: RecentSyncedTabState = RecentSyncedTabState.None,
    val recentBookmarks: List<RecentBookmark> = emptyList(),
    val recentHistory: List<RecentlyVisitedItem> = emptyList(),
    val pendingDeletionHistoryItems: Set<PendingDeletionHistory> = emptySet(),
    val wallpaperState: WallpaperState = WallpaperState.default
) : State
