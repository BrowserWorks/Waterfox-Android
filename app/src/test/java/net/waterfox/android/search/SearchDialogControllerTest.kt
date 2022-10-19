/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.search

import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.BrowserDirection
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.components.Core
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.search.SearchDialogFragmentDirections.Companion.actionGlobalAddonsManagementFragment
import net.waterfox.android.search.SearchDialogFragmentDirections.Companion.actionGlobalSearchEngineFragment
import net.waterfox.android.search.toolbar.SearchSelectorMenu
import net.waterfox.android.settings.SupportUtils
import net.waterfox.android.utils.Settings

@RunWith(WaterfoxRobolectricTestRunner::class)
class SearchDialogControllerTest {

    @MockK(relaxed = true) private lateinit var activity: HomeActivity
    @MockK(relaxed = true) private lateinit var store: SearchDialogFragmentStore
    @MockK(relaxed = true) private lateinit var navController: NavController
    @MockK private lateinit var searchEngine: SearchEngine
    @MockK(relaxed = true) private lateinit var settings: Settings

    private lateinit var middleware: CaptureActionsMiddleware<BrowserState, BrowserAction>
    private lateinit var browserStore: BrowserStore

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        middleware = CaptureActionsMiddleware()
        browserStore = BrowserStore(
            middleware = listOf(middleware)
        )
        every { store.state.tabId } returns "test-tab-id"
        every { store.state.searchEngineSource.searchEngine } returns searchEngine
        every { searchEngine.type } returns SearchEngine.Type.BUNDLED
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.searchDialogFragment
        }
    }

    @Test
    fun handleUrlCommitted() {
        val url = "https://www.google.com/"

        createController().handleUrlCommitted(url)

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = url,
                newTab = false,
                from = BrowserDirection.FromSearchDialog,
                engine = searchEngine
            )
        }
    }

    @Test
    fun handleBlankUrlCommitted() {
        val url = ""

        var dismissDialogInvoked = false
        createController(
            dismissDialog = {
                dismissDialogInvoked = true
            }
        ).handleUrlCommitted(url)

        assertTrue(dismissDialogInvoked)
    }

    @Test
    fun handleSearchCommitted() {
        val searchTerm = "Waterfox"

        createController().handleUrlCommitted(searchTerm)

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = searchTerm,
                newTab = false,
                from = BrowserDirection.FromSearchDialog,
                engine = searchEngine
            )
        }
    }

    @Test
    fun `WHEN the search engine is added by the application THEN do not load URL`() {
        every { searchEngine.type } returns SearchEngine.Type.APPLICATION

        val searchTerm = "Waterfox"
        var dismissDialogInvoked = false

        createController(
            dismissDialog = {
                dismissDialogInvoked = true
            }
        ).handleUrlCommitted(searchTerm)

        verify(exactly = 0) {
            activity.openToBrowserAndLoad(
                searchTermOrURL = any(),
                newTab = any(),
                from = any(),
                engine = any()
            )
        }

        assertFalse(dismissDialogInvoked)
    }

    @Test
    fun handleCrashesUrlCommitted() {
        val url = "about:crashes"
        every { activity.packageName } returns "net.waterfox.android"

        createController().handleUrlCommitted(url)

        verify {
            activity.startActivity(any())
        }
    }

    @Test
    fun handleAddonsUrlCommitted() {
        val url = "about:addons"
        val directions = actionGlobalAddonsManagementFragment()

        createController().handleUrlCommitted(url)

        verify { navController.navigate(directions) }
    }

    @Test
    fun handleMozillaUrlCommitted() {
        val url = "moz://a"

        createController().handleUrlCommitted(url)

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.MANIFESTO),
                newTab = false,
                from = BrowserDirection.FromSearchDialog,
                engine = searchEngine
            )
        }
    }

    @Test
    fun handleEditingCancelled() = runTest {
        var clearToolbarFocusInvoked = false
        createController(
            clearToolbarFocus = {
                clearToolbarFocusInvoked = true
            }
        ).handleEditingCancelled()

        assertTrue(clearToolbarFocusInvoked)
    }

    @Test
    fun handleTextChangedNonEmpty() {
        val text = "waterfox"

        createController().handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.UpdateQuery(text)) }
    }

    @Test
    fun handleTextChangedEmpty() {
        val text = ""

        createController().handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.UpdateQuery(text)) }
    }

    @Test
    fun `show search shortcuts when setting enabled AND query empty`() {
        val text = ""
        every { settings.shouldShowSearchShortcuts } returns true

        createController().handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(true)) }
    }

    @Test
    fun `show search shortcuts when setting enabled AND query equals url`() {
        val text = "mozilla.org"
        every { store.state.url } returns "mozilla.org"
        every { settings.shouldShowSearchShortcuts } returns true

        createController().handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(true)) }
    }

    @Test
    fun `do not show search shortcuts when setting enabled AND query non-empty`() {
        val text = "mozilla"

        createController().handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(false)) }
    }

    @Test
    fun `do not show search shortcuts when setting disabled AND query empty AND url not matching query`() {
        every { settings.shouldShowSearchShortcuts } returns false

        val text = ""

        createController().handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(false)) }
    }

    @Test
    fun `do not show search shortcuts when setting disabled AND query non-empty`() {
        every { settings.shouldShowSearchShortcuts } returns false

        val text = "mozilla"

        createController().handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(false)) }
    }

    @Test
    fun handleUrlTapped() {
        val url = "https://www.google.com/"
        val flags = EngineSession.LoadUrlFlags.all()

        createController().handleUrlTapped(url, flags)
        createController().handleUrlTapped(url)

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = url,
                newTab = false,
                from = BrowserDirection.FromSearchDialog,
                flags = flags
            )
        }
    }

    @Test
    fun handleSearchTermsTapped() {
        val searchTerms = "waterfox"

        createController().handleSearchTermsTapped(searchTerms)

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = searchTerms,
                newTab = false,
                from = BrowserDirection.FromSearchDialog,
                engine = searchEngine,
                forceSearch = true
            )
        }
    }

    @Test
    fun handleSearchShortcutEngineSelected() {
        val searchEngine: SearchEngine = mockk(relaxed = true)

        var focusToolbarInvoked = false
        createController(
            focusToolbar = {
                focusToolbarInvoked = true
            }
        ).handleSearchShortcutEngineSelected(searchEngine)

        assertTrue(focusToolbarInvoked)
        verify { store.dispatch(SearchFragmentAction.SearchShortcutEngineSelected(searchEngine, settings)) }
    }

    @Test
    fun `WHEN history search engine is selected THEN dispatch correct action`() {
        val searchEngine: SearchEngine = mockk(relaxed = true)
        every { searchEngine.type } returns SearchEngine.Type.APPLICATION
        every { searchEngine.id } returns Core.HISTORY_SEARCH_ENGINE_ID
        every { settings.showUnifiedSearchFeature } returns true

        var focusToolbarInvoked = false
        createController(
            focusToolbar = {
                focusToolbarInvoked = true
            }
        ).handleSearchShortcutEngineSelected(searchEngine)

        assertTrue(focusToolbarInvoked)
        verify { store.dispatch(SearchFragmentAction.SearchHistoryEngineSelected(searchEngine)) }
    }

    @Test
    fun `WHEN bookmarks search engine is selected THEN dispatch correct action`() {
        val searchEngine: SearchEngine = mockk(relaxed = true)
        every { searchEngine.type } returns SearchEngine.Type.APPLICATION
        every { searchEngine.id } returns Core.BOOKMARKS_SEARCH_ENGINE_ID
        every { settings.showUnifiedSearchFeature } returns true

        var focusToolbarInvoked = false
        createController(
            focusToolbar = {
                focusToolbarInvoked = true
            }
        ).handleSearchShortcutEngineSelected(searchEngine)

        assertTrue(focusToolbarInvoked)
        verify { store.dispatch(SearchFragmentAction.SearchBookmarksEngineSelected(searchEngine)) }
    }

    @Test
    fun `WHEN tabs search engine is selected THEN dispatch correct action`() {
        val searchEngine: SearchEngine = mockk(relaxed = true)
        every { searchEngine.type } returns SearchEngine.Type.APPLICATION
        every { searchEngine.id } returns Core.TABS_SEARCH_ENGINE_ID
        every { settings.showUnifiedSearchFeature } returns true

        var focusToolbarInvoked = false
        createController(
            focusToolbar = {
                focusToolbarInvoked = true
            }
        ).handleSearchShortcutEngineSelected(searchEngine)

        assertTrue(focusToolbarInvoked)
        verify { store.dispatch(SearchFragmentAction.SearchTabsEngineSelected(searchEngine)) }
    }

    @Test
    fun handleClickSearchEngineSettings() {
        val directions: NavDirections = actionGlobalSearchEngineFragment()

        createController().handleClickSearchEngineSettings()

        verify { navController.navigate(directions) }
    }

    @Test
    fun handleSearchShortcutsButtonClicked_alreadyOpen() {
        every { store.state.showSearchShortcuts } returns true

        createController().handleSearchShortcutsButtonClicked()

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(false)) }
    }

    @Test
    fun handleSearchShortcutsButtonClicked_notYetOpen() {
        every { store.state.showSearchShortcuts } returns false

        createController().handleSearchShortcutsButtonClicked()

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(true)) }
    }

    @Test
    fun handleExistingSessionSelected() {
        createController().handleExistingSessionSelected("selected")

        browserStore.waitUntilIdle()

        middleware.assertFirstAction(TabListAction.SelectTabAction::class) { action ->
            assertEquals("selected", action.tabId)
        }

        verify { activity.openToBrowser(from = BrowserDirection.FromSearchDialog) }
    }

    @Test
    fun handleExistingSessionSelected_tabId() {
        createController().handleExistingSessionSelected("tab-id")

        browserStore.waitUntilIdle()

        middleware.assertFirstAction(TabListAction.SelectTabAction::class) { action ->
            assertEquals("tab-id", action.tabId)
        }
        verify { activity.openToBrowser(from = BrowserDirection.FromSearchDialog) }
    }

    @Test
    fun `show camera permissions needed dialog`() {
        val dialogBuilder: AlertDialog.Builder = mockk(relaxed = true)

        val spyController = spyk(createController())
        every { spyController.buildDialog() } returns dialogBuilder

        spyController.handleCameraPermissionsNeeded()

        verify { dialogBuilder.show() }
    }

    @Test
    fun `GIVEN search settings menu item WHEN search selector menu item is tapped THEN show search engine settings`() {
        val controller = spyk(createController())

        controller.handleMenuItemTapped(SearchSelectorMenu.Item.SearchSettings)

        verify { controller.handleClickSearchEngineSettings() }
    }

    private fun createController(
        clearToolbarFocus: () -> Unit = { },
        focusToolbar: () -> Unit = { },
        clearToolbar: () -> Unit = { },
        dismissDialog: () -> Unit = { }
    ): SearchDialogController {
        return SearchDialogController(
            activity = activity,
            store = browserStore,
            tabsUseCases = TabsUseCases(browserStore),
            fragmentStore = store,
            navController = navController,
            settings = settings,
            dismissDialog = dismissDialog,
            clearToolbarFocus = clearToolbarFocus,
            focusToolbar = focusToolbar,
            clearToolbar = clearToolbar
        )
    }
}
