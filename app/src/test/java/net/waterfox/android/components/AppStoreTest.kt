/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.components

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mozilla.components.concept.sync.DeviceType
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.browser.browsingmode.BrowsingModeManager
import net.waterfox.android.components.appstate.AppAction
import net.waterfox.android.components.appstate.AppAction.MessagingAction.UpdateMessageToShow
import net.waterfox.android.components.appstate.AppState
import net.waterfox.android.components.appstate.filterOut
import net.waterfox.android.ext.components
import net.waterfox.android.home.CurrentMode
import net.waterfox.android.home.Mode
import net.waterfox.android.home.recentbookmarks.RecentBookmark
import net.waterfox.android.home.recentsyncedtabs.RecentSyncedTab
import net.waterfox.android.home.recentsyncedtabs.RecentSyncedTabState
import net.waterfox.android.home.recenttabs.RecentTab
import net.waterfox.android.home.recentvisits.RecentlyVisitedItem
import net.waterfox.android.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import net.waterfox.android.home.recentvisits.RecentlyVisitedItem.RecentHistoryHighlight
import net.waterfox.android.onboarding.WaterfoxOnboarding

class AppStoreTest {
    private lateinit var context: Context
    private lateinit var accountManager: FxaAccountManager
    private lateinit var onboarding: WaterfoxOnboarding
    private lateinit var browsingModeManager: BrowsingModeManager
    private lateinit var currentMode: CurrentMode
    private lateinit var appState: AppState
    private lateinit var appStore: AppStore

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        accountManager = mockk(relaxed = true)
        onboarding = mockk(relaxed = true)
        browsingModeManager = mockk(relaxed = true)

        every { context.components.backgroundServices.accountManager } returns accountManager
        every { onboarding.userHasBeenOnboarded() } returns true
        every { browsingModeManager.mode } returns BrowsingMode.Normal

        currentMode = CurrentMode(
            context,
            onboarding,
            browsingModeManager
        ) {}

        appState = AppState(
            collections = emptyList(),
            expandedCollections = emptySet(),
            mode = currentMode.getCurrentMode(),
            topSites = emptyList(),
            showCollectionPlaceholder = true,
            recentTabs = emptyList()
        )

        appStore = AppStore(appState)
    }

    @Test
    fun `Test toggling the mode in AppStore`() = runTest {
        // Verify that the default mode and tab states of the HomeFragment are correct.
        assertEquals(Mode.Normal, appStore.state.mode)

        // Change the AppStore to Private mode.
        appStore.dispatch(AppAction.ModeChange(Mode.Private)).join()
        assertEquals(Mode.Private, appStore.state.mode)

        // Change the AppStore back to Normal mode.
        appStore.dispatch(AppAction.ModeChange(Mode.Normal)).join()
        assertEquals(Mode.Normal, appStore.state.mode)
    }

    @Test
    fun `GIVEN a new value for messageToShow WHEN NimbusMessageChange is called THEN update the current value`() =
        runTest {
            assertNull(appStore.state.messaging.messageToShow)

            appStore.dispatch(UpdateMessageToShow(mockk())).join()

            assertNotNull(appStore.state.messaging.messageToShow)
        }

    @Test
    fun `Test changing the collections in AppStore`() = runTest {
        assertEquals(0, appStore.state.collections.size)

        // Add 2 TabCollections to the AppStore.
        val tabCollections: List<TabCollection> = listOf(mockk(), mockk())
        appStore.dispatch(AppAction.CollectionsChange(tabCollections)).join()

        assertEquals(tabCollections, appStore.state.collections)
    }

    @Test
    fun `Test changing the top sites in AppStore`() = runTest {
        assertEquals(0, appStore.state.topSites.size)

        // Add 2 TopSites to the AppStore.
        val topSites: List<TopSite> = listOf(mockk(), mockk())
        appStore.dispatch(AppAction.TopSitesChange(topSites)).join()

        assertEquals(topSites, appStore.state.topSites)
    }

    @Test
    fun `Test changing the recent tabs in AppStore`() = runTest {
        val group1 = RecentHistoryGroup(title = "title1")
        val group2 = RecentHistoryGroup(title = "title2")
        val group3 = RecentHistoryGroup(title = "title3")
        val highlight = RecentHistoryHighlight(title = group2.title, "")
        appStore = AppStore(
            AppState(
                recentHistory = listOf(group1, group2, group3, highlight)
            )
        )
        assertEquals(0, appStore.state.recentTabs.size)

        // Add 2 RecentTabs to the AppStore
        val recentTab1: RecentTab.Tab = mockk()
        val recentTabs: List<RecentTab> = listOf(recentTab1)
        appStore.dispatch(AppAction.RecentTabsChange(recentTabs)).join()

        assertEquals(recentTabs, appStore.state.recentTabs)
        assertEquals(listOf(group1, group2, group3, highlight), appStore.state.recentHistory)
    }

    @Test
    fun `GIVEN initial state WHEN recent synced tab state is changed THEN state updated`() = runTest {
        appStore = AppStore(
            AppState(
                recentSyncedTabState = RecentSyncedTabState.None
            )
        )

        val loading = RecentSyncedTabState.Loading
        appStore.dispatch(AppAction.RecentSyncedTabStateChange(loading)).join()
        assertEquals(loading, appStore.state.recentSyncedTabState)

        val recentSyncedTab = RecentSyncedTab("device name", DeviceType.DESKTOP, "title", "url", null)
        val success = RecentSyncedTabState.Success(recentSyncedTab)
        appStore.dispatch(AppAction.RecentSyncedTabStateChange(success)).join()
        assertEquals(success, appStore.state.recentSyncedTabState)
        assertEquals(recentSyncedTab, (appStore.state.recentSyncedTabState as RecentSyncedTabState.Success).tab)
    }

    @Test
    fun `Test changing the history metadata in AppStore`() = runTest {
        assertEquals(0, appStore.state.recentHistory.size)

        val historyMetadata: List<RecentHistoryGroup> = listOf(mockk(), mockk())
        appStore.dispatch(AppAction.RecentHistoryChange(historyMetadata)).join()

        assertEquals(historyMetadata, appStore.state.recentHistory)
    }

    @Test
    fun `Test removing a history highlight from AppStore`() = runTest {
        val g1 = RecentHistoryGroup(title = "group One")
        val g2 = RecentHistoryGroup(title = "grup two")
        val h1 = RecentHistoryHighlight(title = "highlight One", url = "url1")
        val h2 = RecentHistoryHighlight(title = "highlight two", url = "url2")
        val recentHistoryState = AppState(
            recentHistory = listOf(g1, g2, h1, h2)
        )
        appStore = AppStore(recentHistoryState)

        appStore.dispatch(AppAction.RemoveRecentHistoryHighlight("invalid")).join()
        assertEquals(recentHistoryState, appStore.state)

        appStore.dispatch(AppAction.RemoveRecentHistoryHighlight(h1.title)).join()
        assertEquals(recentHistoryState, appStore.state)

        appStore.dispatch(AppAction.RemoveRecentHistoryHighlight(h1.url)).join()
        assertEquals(
            recentHistoryState.copy(recentHistory = listOf(g1, g2, h2)),
            appStore.state
        )
    }

    @Test
    fun `Test disbanding search group in AppStore`() = runTest {
        val g1 = RecentHistoryGroup(title = "test One")
        val g2 = RecentHistoryGroup(title = "test two")
        val h1 = RecentHistoryHighlight(title = "highlight One", url = "url1")
        val h2 = RecentHistoryHighlight(title = "highlight two", url = "url2")
        val recentHistory: List<RecentlyVisitedItem> = listOf(g1, g2, h1, h2)
        appStore.dispatch(AppAction.RecentHistoryChange(recentHistory)).join()
        assertEquals(recentHistory, appStore.state.recentHistory)

        appStore.dispatch(AppAction.DisbandSearchGroupAction("Test one")).join()
        assertEquals(listOf(g2, h1, h2), appStore.state.recentHistory)
    }

    @Test
    fun `Test changing hiding collections placeholder`() = runTest {
        assertTrue(appStore.state.showCollectionPlaceholder)

        appStore.dispatch(AppAction.RemoveCollectionsPlaceholder).join()

        assertFalse(appStore.state.showCollectionPlaceholder)
    }

    @Test
    fun `Test changing the expanded collections in AppStore`() = runTest {
        val collection: TabCollection = mockk<TabCollection>().apply {
            every { id } returns 0
        }

        // Expand the given collection.
        appStore.dispatch(AppAction.CollectionsChange(listOf(collection))).join()
        appStore.dispatch(AppAction.CollectionExpanded(collection, true)).join()

        assertTrue(appStore.state.expandedCollections.contains(collection.id))
        assertEquals(1, appStore.state.expandedCollections.size)
    }

    @Test
    fun `Test changing the collections, mode, recent tabs and bookmarks, history metadata and top sites in the AppStore`() =
        runTest {
            // Verify that the default state of the HomeFragment is correct.
            assertEquals(0, appStore.state.collections.size)
            assertEquals(0, appStore.state.topSites.size)
            assertEquals(0, appStore.state.recentTabs.size)
            assertEquals(0, appStore.state.recentBookmarks.size)
            assertEquals(0, appStore.state.recentHistory.size)
            assertEquals(Mode.Normal, appStore.state.mode)

            val collections: List<TabCollection> = listOf(mockk())
            val topSites: List<TopSite> = listOf(mockk(), mockk())
            val recentTabs: List<RecentTab> = listOf(mockk(), mockk())
            val recentBookmarks: List<RecentBookmark> = listOf(mockk(), mockk())
            val group1 = RecentHistoryGroup(title = "test One")
            val group2 = RecentHistoryGroup(title = "testSearchTerm")
            val group3 = RecentHistoryGroup(title = "test two")
            val highlight = RecentHistoryHighlight(group2.title, "")
            val recentHistory: List<RecentlyVisitedItem> = listOf(group1, group2, group3, highlight)

            appStore.dispatch(
                AppAction.Change(
                    collections = collections,
                    mode = Mode.Private,
                    topSites = topSites,
                    showCollectionPlaceholder = true,
                    recentTabs = recentTabs,
                    recentBookmarks = recentBookmarks,
                    recentHistory = recentHistory
                )
            ).join()

            assertEquals(collections, appStore.state.collections)
            assertEquals(topSites, appStore.state.topSites)
            assertEquals(recentTabs, appStore.state.recentTabs)
            assertEquals(recentBookmarks, appStore.state.recentBookmarks)
            assertEquals(listOf(group1, group2, group3, highlight), appStore.state.recentHistory)
            assertEquals(Mode.Private, appStore.state.mode)
        }

    @Test
    fun `Test filtering out search groups`() {
        val group1 = RecentHistoryGroup("title1")
        val group2 = RecentHistoryGroup("title2")
        val group3 = RecentHistoryGroup("title3")
        val highLight1 = RecentHistoryHighlight("title1", "")
        val highLight2 = RecentHistoryHighlight("title2", "")
        val highLight3 = RecentHistoryHighlight("title3", "")
        val recentHistory = listOf(group1, highLight1, group2, highLight2, group3, highLight3)

        assertEquals(recentHistory, recentHistory.filterOut(null))
        assertEquals(recentHistory, recentHistory.filterOut(""))
        assertEquals(recentHistory, recentHistory.filterOut(" "))
        assertEquals(recentHistory - group2, recentHistory.filterOut("Title2"))
        assertEquals(recentHistory - group3, recentHistory.filterOut("title3"))
    }
}
