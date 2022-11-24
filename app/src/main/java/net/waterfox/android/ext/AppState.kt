/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ext

import net.waterfox.android.components.appstate.AppState
import net.waterfox.android.home.blocklist.BlocklistHandler
import net.waterfox.android.home.recentsyncedtabs.RecentSyncedTabState
import net.waterfox.android.utils.Settings

/**
 * Filter a [AppState] by the blocklist.
 *
 * @param blocklistHandler The handler that will filter the state.
 */
fun AppState.filterState(blocklistHandler: BlocklistHandler): AppState =
    with(blocklistHandler) {
        copy(
            recentBookmarks = recentBookmarks.filteredByBlocklist(),
            recentTabs = recentTabs.filteredByBlocklist().filterContile(),
            recentHistory = recentHistory.filteredByBlocklist().filterContile(),
            recentSyncedTabState = recentSyncedTabState.filteredByBlocklist().filterContile()
        )
    }

/**
 * Determines whether a recent tab section should be shown, based on user preference
 * and the availability of local or Synced tabs.
 */
fun AppState.shouldShowRecentTabs(settings: Settings): Boolean {
    val hasTab = recentTabs.isNotEmpty() || recentSyncedTabState is RecentSyncedTabState.Success
    return settings.showRecentTabsFeature && hasTab
}


/**
 * Determines whether a recent synced tab section should be shown, based on user preference
 * and the availability of Synced tabs.
 */
fun AppState.shouldShowRecentSyncedTabs(settings: Settings): Boolean {
    return settings.enableTaskContinuityEnhancements && recentSyncedTabState is RecentSyncedTabState.Success
}
