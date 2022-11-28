/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.sessioncontrol

import androidx.recyclerview.widget.RecyclerView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.components.appstate.AppState
import net.waterfox.android.ext.components
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.home.recentbookmarks.RecentBookmark
import net.waterfox.android.home.recenttabs.RecentTab
import net.waterfox.android.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import net.waterfox.android.utils.Settings

@RunWith(WaterfoxRobolectricTestRunner::class)
class SessionControlViewTest {

    @Test
    fun `GIVEN recent Bookmarks WHEN normalModeAdapterItems is called THEN add a customize home button`() {
        val settings: Settings = mockk()
        val topSites = emptyList<TopSite>()
        val collections = emptyList<TabCollection>()
        val expandedCollections = emptySet<Long>()
        val recentBookmarks = listOf(RecentBookmark())
        val historyMetadata = emptyList<RecentHistoryGroup>()

        every { settings.showTopSitesFeature } returns true
        every { settings.showRecentTabsFeature } returns true
        every { settings.showRecentBookmarksFeature } returns true
        every { settings.historyMetadataUIFeature } returns true

        val results = normalModeAdapterItems(
            settings,
            topSites,
            collections,
            expandedCollections,
            recentBookmarks,
            false,
            false,
            showRecentSyncedTab = false,
            recentVisits = historyMetadata
        )

        assertTrue(results[0] is AdapterItem.TopPlaceholderItem)
        assertTrue(results[1] is AdapterItem.RecentBookmarksHeader)
        assertTrue(results[2] is AdapterItem.RecentBookmarks)
        assertTrue(results[3] is AdapterItem.CustomizeHomeButton)
    }

    @Test
    fun `GIVEN recent tabs WHEN normalModeAdapterItems is called THEN add a customize home button`() {
        val settings: Settings = mockk()
        val topSites = emptyList<TopSite>()
        val collections = emptyList<TabCollection>()
        val expandedCollections = emptySet<Long>()
        val recentBookmarks = listOf<RecentBookmark>()
        val historyMetadata = emptyList<RecentHistoryGroup>()

        every { settings.showTopSitesFeature } returns true
        every { settings.showRecentTabsFeature } returns true
        every { settings.showRecentBookmarksFeature } returns true
        every { settings.historyMetadataUIFeature } returns true

        val results = normalModeAdapterItems(
            settings,
            topSites,
            collections,
            expandedCollections,
            recentBookmarks,
            false,
            true,
            showRecentSyncedTab = false,
            historyMetadata
        )

        assertTrue(results[0] is AdapterItem.TopPlaceholderItem)
        assertTrue(results[1] is AdapterItem.RecentTabsHeader)
        assertTrue(results[2] is AdapterItem.RecentTabItem)
        assertTrue(results[3] is AdapterItem.CustomizeHomeButton)
    }

    @Test
    fun `GIVEN history metadata WHEN normalModeAdapterItems is called THEN add a customize home button`() {
        val settings: Settings = mockk()
        val topSites = emptyList<TopSite>()
        val collections = emptyList<TabCollection>()
        val expandedCollections = emptySet<Long>()
        val recentBookmarks = listOf<RecentBookmark>()
        val historyMetadata = listOf(RecentHistoryGroup("title", emptyList()))

        every { settings.showTopSitesFeature } returns true
        every { settings.showRecentTabsFeature } returns true
        every { settings.showRecentBookmarksFeature } returns true
        every { settings.historyMetadataUIFeature } returns true

        val results = normalModeAdapterItems(
            settings,
            topSites,
            collections,
            expandedCollections,
            recentBookmarks,
            false,
            false,
            showRecentSyncedTab = false,
            historyMetadata,
            true
        )

        assertTrue(results[0] is AdapterItem.TopPlaceholderItem)
        assertTrue(results[1] is AdapterItem.RecentVisitsHeader)
        assertTrue(results[2] is AdapterItem.RecentVisitsItems)
        assertTrue(results[3] is AdapterItem.CustomizeHomeButton)
    }

    @Test
    fun `GIVEN none recentBookmarks, recentTabs or historyMetadata WHEN normalModeAdapterItems is called THEN the customize home button is not added`() {
        val settings: Settings = mockk()
        val topSites = emptyList<TopSite>()
        val collections = emptyList<TabCollection>()
        val expandedCollections = emptySet<Long>()
        val recentBookmarks = listOf<RecentBookmark>()
        val historyMetadata = emptyList<RecentHistoryGroup>()

        every { settings.showTopSitesFeature } returns true
        every { settings.showRecentTabsFeature } returns true
        every { settings.showRecentBookmarksFeature } returns true
        every { settings.historyMetadataUIFeature } returns true

        val results = normalModeAdapterItems(
            settings,
            topSites,
            collections,
            expandedCollections,
            recentBookmarks,
            false,
            false,
            showRecentSyncedTab = false,
            historyMetadata
        )
        assertEquals(results.size, 2)
        assertTrue(results[0] is AdapterItem.TopPlaceholderItem)
    }

    @Test
    fun `GIVEN all items THEN top placeholder item is always the first item`() {
        val settings: Settings = mockk()
        val collection = mockk<TabCollection> {
            every { id } returns 123L
        }
        val topSites = listOf<TopSite>(mockk())
        val collections = listOf(collection)
        val expandedCollections = emptySet<Long>()
        val recentBookmarks = listOf<RecentBookmark>(mockk())
        val historyMetadata = listOf<RecentHistoryGroup>(mockk())

        every { settings.showTopSitesFeature } returns true
        every { settings.showRecentTabsFeature } returns true
        every { settings.showRecentBookmarksFeature } returns true
        every { settings.historyMetadataUIFeature } returns true

        val results = normalModeAdapterItems(
            settings,
            topSites,
            collections,
            expandedCollections,
            recentBookmarks,
            false,
            true,
            showRecentSyncedTab = true,
            historyMetadata
        )

        assertTrue(results[0] is AdapterItem.TopPlaceholderItem)
    }
}
