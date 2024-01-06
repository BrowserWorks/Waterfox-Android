/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.TabEntry
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import net.waterfox.android.BrowserDirection
import net.waterfox.android.HomeActivity
import net.waterfox.android.collections.CollectionsDialog
import net.waterfox.android.collections.show
import net.waterfox.android.components.TabCollectionStorage
import net.waterfox.android.components.accounts.WaterfoxFxAEntryPoint
import net.waterfox.android.components.bookmarks.BookmarksUseCase
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mozilla.components.browser.state.state.createTab as createStateTab
import mozilla.components.browser.storage.sync.Tab as SyncTab

@RunWith(WaterfoxRobolectricTestRunner::class)
class NavigationInteractorTest {
    private lateinit var store: BrowserStore
    private lateinit var tabsTrayStore: TabsTrayStore
    private val testTab: TabSessionState = createStateTab(url = "https://mozilla.org")
    private val navController: NavController = mockk(relaxed = true)
    private val bookmarksUseCase: BookmarksUseCase = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val collectionStorage: TabCollectionStorage = mockk(relaxed = true)
    private val accountManager: FxaAccountManager = mockk(relaxed = true)
    private val activity: HomeActivity = mockk(relaxed = true)

    @get:Rule
    val coroutinesTestRule: MainCoroutineRule = MainCoroutineRule()

    private val testDispatcher = coroutinesTestRule.testDispatcher

    @Before
    fun setup() {
        store = BrowserStore(initialState = BrowserState(tabs = listOf(testTab)))
        tabsTrayStore = TabsTrayStore()
    }

    @Test
    fun `onTabTrayDismissed calls dismissTabTray on DefaultNavigationInteractor`() {
        var dismissTabTrayInvoked = false

        createInteractor(
            dismissTabTray = {
                dismissTabTrayInvoked = true
            }
        ).onTabTrayDismissed()

        assertTrue(dismissTabTrayInvoked)
    }

    @Test
    fun `onAccountSettingsClicked calls navigation on DefaultNavigationInteractor`() {
        every { accountManager.authenticatedAccount() }.answers { mockk(relaxed = true) }

        createInteractor().onAccountSettingsClicked()

        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalAccountSettingsFragment()) }
    }

    @Test
    fun `onAccountSettingsClicked when not logged in calls navigation to turn on sync`() {
        every { accountManager.authenticatedAccount() }.answers { null }

        createInteractor().onAccountSettingsClicked()

        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalTurnOnSync(
            entrypoint = WaterfoxFxAEntryPoint.NavigationInteraction,
        )) }
    }

    @Test
    fun `onTabSettingsClicked calls navigation on DefaultNavigationInteractor`() {
        createInteractor().onTabSettingsClicked()
        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalTabSettingsFragment()) }
    }

    @Test
    fun `onOpenRecentlyClosedClicked calls navigation on DefaultNavigationInteractor`() {
        createInteractor().onOpenRecentlyClosedClicked()

        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalRecentlyClosed()) }
    }

    @Test
    fun `onCloseAllTabsClicked calls navigation on DefaultNavigationInteractor`() {
        var dismissTabTrayAndNavigateHomeInvoked = false
        createInteractor(
            dismissTabTrayAndNavigateHome = {
                dismissTabTrayAndNavigateHomeInvoked = true
            }
        ).onCloseAllTabsClicked(false)

        assertTrue(dismissTabTrayAndNavigateHomeInvoked)
    }

    @Test
    fun `GIVEN active private download WHEN onCloseAllTabsClicked is called for private tabs THEN showCancelledDownloadWarning is called`() {
        var showCancelledDownloadWarningInvoked = false
        val mockedStore: BrowserStore = mockk()
        val controller = spyk(
            createInteractor(
                browserStore = mockedStore,
                showCancelledDownloadWarning = { _, _, _ ->
                    showCancelledDownloadWarningInvoked = true
                }
            )
        )
        val tab: TabSessionState = mockk { every { content.private } returns true }
        every { mockedStore.state } returns mockk()
        every { mockedStore.state.downloads } returns mapOf(
            "1" to DownloadState(
                "https://mozilla.org/download",
                private = true,
                destinationDirectory = "Download",
                status = DownloadState.Status.DOWNLOADING
            )
        )
        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { mockedStore.state.findTab(any()) } returns tab
            every { mockedStore.state.getNormalOrPrivateTabs(any()) } returns listOf(tab)

            controller.onCloseAllTabsClicked(true)

            assertTrue(showCancelledDownloadWarningInvoked)
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `onShareTabsOfType calls navigation on DefaultNavigationInteractor`() {
        createInteractor().onShareTabsOfTypeClicked(false)
        verify(exactly = 1) { navController.navigate(any<NavDirections>()) }
    }

    @Test
    fun `onShareTabs calls navigation on DefaultNavigationInteractor`() {
        createInteractor().onShareTabs(listOf(testTab))

        verify(exactly = 1) { navController.navigate(any<NavDirections>()) }
    }

    @Test
    fun `onSaveToCollections calls navigation on DefaultNavigationInteractor`() {
        mockkStatic("net.waterfox.android.collections.CollectionsDialogKt")

        every { any<CollectionsDialog>().show(any()) } answers { }

        createInteractor().onSaveToCollections(listOf(testTab))

        // TODO: [Waterfox] verify navigation

        unmockkStatic("net.waterfox.android.collections.CollectionsDialogKt")
    }

    @Test
    fun `onBookmarkTabs calls navigation on DefaultNavigationInteractor`() = runTestOnMain {
        var showBookmarkSnackbarInvoked = false
        createInteractor(
            showBookmarkSnackbar = {
                showBookmarkSnackbarInvoked = true
            }
        ).onSaveToBookmarks(listOf(createStateTab("url")))

        coVerify(exactly = 1) { bookmarksUseCase.addBookmark(any(), any(), any()) }
        assertTrue(showBookmarkSnackbarInvoked)
    }

    @Test
    fun `onSyncedTabsClicked opens browser`() {
        val tab = mockk<SyncTab>()
        val entry = mockk<TabEntry>()

        every { tab.active() }.answers { entry }
        every { entry.url }.answers { "https://mozilla.org" }

        var dismissTabTrayInvoked = false
        createInteractor(
            dismissTabTray = {
                dismissTabTrayInvoked = true
            }
        ).onSyncedTabClicked(tab)

        assertTrue(dismissTabTrayInvoked)

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = "https://mozilla.org",
                newTab = true,
                from = BrowserDirection.FromTabsTray
            )
        }
    }

    @Suppress("LongParameterList")
    private fun createInteractor(
        browserStore: BrowserStore = store,
        dismissTabTray: () -> Unit = { },
        dismissTabTrayAndNavigateHome: (String) -> Unit = { _ -> },
        showCollectionSnackbar: (Int, Boolean) -> Unit = { _, _ -> },
        showBookmarkSnackbar: (Int) -> Unit = { _ -> },
        showCancelledDownloadWarning: (Int, String?, String?) -> Unit = { _, _, _ -> }
    ): NavigationInteractor {
        return DefaultNavigationInteractor(
            context,
            activity,
            browserStore,
            navController,
            dismissTabTray,
            dismissTabTrayAndNavigateHome,
            bookmarksUseCase,
            tabsTrayStore,
            collectionStorage,
            showCollectionSnackbar,
            showBookmarkSnackbar,
            showCancelledDownloadWarning,
            accountManager,
            testDispatcher
        )
    }
}
