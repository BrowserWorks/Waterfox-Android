/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home

import io.mockk.mockk
import io.mockk.verify
import mozilla.components.feature.tab.collections.Tab
import mozilla.components.feature.tab.collections.TabCollection
import org.junit.Before
import org.junit.Test
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.home.recentbookmarks.RecentBookmark
import net.waterfox.android.home.recentbookmarks.controller.RecentBookmarksController
import net.waterfox.android.home.recentsyncedtabs.RecentSyncedTab
import net.waterfox.android.home.recentsyncedtabs.controller.RecentSyncedTabController
import net.waterfox.android.home.recenttabs.controller.RecentTabController
import net.waterfox.android.home.recentvisits.controller.RecentVisitsController
import net.waterfox.android.home.sessioncontrol.DefaultSessionControlController
import net.waterfox.android.home.sessioncontrol.SessionControlInteractor

class SessionControlInteractorTest {

    private val controller: DefaultSessionControlController = mockk(relaxed = true)
    private val recentTabController: RecentTabController = mockk(relaxed = true)
    private val recentSyncedTabController: RecentSyncedTabController = mockk(relaxed = true)
    private val recentBookmarksController: RecentBookmarksController = mockk(relaxed = true)

    // Note: the recent visits tests are handled in [RecentVisitsInteractorTest] and [RecentVisitsControllerTest]
    private val recentVisitsController: RecentVisitsController = mockk(relaxed = true)

    private lateinit var interactor: SessionControlInteractor

    @Before
    fun setup() {
        interactor = SessionControlInteractor(
            controller,
            recentTabController,
            recentSyncedTabController,
            recentBookmarksController,
            recentVisitsController
        )
    }

    @Test
    fun onCollectionAddTabTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onCollectionAddTabTapped(collection)
        verify { controller.handleCollectionAddTabTapped(collection) }
    }

    @Test
    fun onCollectionOpenTabClicked() {
        val tab: Tab = mockk(relaxed = true)
        interactor.onCollectionOpenTabClicked(tab)
        verify { controller.handleCollectionOpenTabClicked(tab) }
    }

    @Test
    fun onCollectionOpenTabsTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onCollectionOpenTabsTapped(collection)
        verify { controller.handleCollectionOpenTabsTapped(collection) }
    }

    @Test
    fun onCollectionRemoveTab() {
        val collection: TabCollection = mockk(relaxed = true)
        val tab: Tab = mockk(relaxed = true)
        interactor.onCollectionRemoveTab(collection, tab, false)
        verify { controller.handleCollectionRemoveTab(collection, tab, false) }
    }

    @Test
    fun onCollectionShareTabsClicked() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onCollectionShareTabsClicked(collection)
        verify { controller.handleCollectionShareTabsClicked(collection) }
    }

    @Test
    fun onDeleteCollectionTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onDeleteCollectionTapped(collection)
        verify { controller.handleDeleteCollectionTapped(collection) }
    }

    @Test
    fun onPrivateBrowsingLearnMoreClicked() {
        interactor.onPrivateBrowsingLearnMoreClicked()
        verify { controller.handlePrivateBrowsingLearnMoreClicked() }
    }

    @Test
    fun onRenameCollectionTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onRenameCollectionTapped(collection)
        verify { controller.handleRenameCollectionTapped(collection) }
    }

    @Test
    fun onStartBrowsingClicked() {
        interactor.onStartBrowsingClicked()
        verify { controller.handleStartBrowsingClicked() }
    }

    @Test
    fun onToggleCollectionExpanded() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onToggleCollectionExpanded(collection, true)
        verify { controller.handleToggleCollectionExpanded(collection, true) }
    }

    @Test
    fun onAddTabsToCollection() {
        interactor.onAddTabsToCollectionTapped()
        verify { controller.handleCreateCollection() }
    }

    @Test
    fun onPaste() {
        interactor.onPaste("text")
        verify { controller.handlePaste("text") }
    }

    @Test
    fun onPasteAndGo() {
        interactor.onPasteAndGo("text")
        verify { controller.handlePasteAndGo("text") }
    }

    @Test
    fun onRemoveCollectionsPlaceholder() {
        interactor.onRemoveCollectionsPlaceholder()
        verify { controller.handleRemoveCollectionsPlaceholder() }
    }

    @Test
    fun onCollectionMenuOpened() {
        interactor.onCollectionMenuOpened()
        verify { controller.handleMenuOpened() }
    }

    @Test
    fun onTopSiteMenuOpened() {
        interactor.onTopSiteMenuOpened()
        verify { controller.handleMenuOpened() }
    }

    @Test
    fun onRecentTabClicked() {
        val tabId = "tabId"
        interactor.onRecentTabClicked(tabId)
        verify { recentTabController.handleRecentTabClicked(tabId) }
    }

    @Test
    fun onRecentTabShowAllClicked() {
        interactor.onRecentTabShowAllClicked()
        verify { recentTabController.handleRecentTabShowAllClicked() }
    }

    @Test
    fun `WHEN recent synced tab is clicked THEN the tab is handled`() {
        val tab: RecentSyncedTab = mockk()
        interactor.onRecentSyncedTabClicked(tab)

        verify { recentSyncedTabController.handleRecentSyncedTabClick(tab) }
    }

    @Test
    fun `WHEN recent synced tabs show all is clicked THEN show all synced tabs is handled`() {
        interactor.onSyncedTabShowAllClicked()

        verify { recentSyncedTabController.handleSyncedTabShowAllClicked() }
    }

    @Test
    fun `WHEN a recently saved bookmark is clicked THEN the selected bookmark is handled`() {
        val bookmark = RecentBookmark()

        interactor.onRecentBookmarkClicked(bookmark)
        verify { recentBookmarksController.handleBookmarkClicked(bookmark) }
    }

    @Test
    fun `WHEN tapping on the customize home button THEN openCustomizeHomePage`() {
        interactor.openCustomizeHomePage()
        verify { controller.handleCustomizeHomeTapped() }
    }

    @Test
    fun `WHEN calling showOnboardingDialog THEN handleShowOnboardingDialog`() {
        interactor.showOnboardingDialog()
        verify { controller.handleShowOnboardingDialog() }
    }

    @Test
    fun `WHEN Show All recently saved bookmarks button is clicked THEN the click is handled`() {
        interactor.onShowAllBookmarksClicked()
        verify { recentBookmarksController.handleShowAllBookmarksClicked() }
    }

    @Test
    fun `WHEN private mode button is clicked THEN the click is handled`() {
        val newMode = BrowsingMode.Private
        val hasBeenOnboarded = true

        interactor.onPrivateModeButtonClicked(newMode, hasBeenOnboarded)
        verify { controller.handlePrivateModeButtonClicked(newMode, hasBeenOnboarded) }
    }

    @Test
    fun `WHEN onSettingsClicked is called THEN handleTopSiteSettingsClicked is called`() {
        interactor.onSettingsClicked()
        verify { controller.handleTopSiteSettingsClicked() }
    }

    @Test
    fun `WHEN onSponsorPrivacyClicked is called THEN handleSponsorPrivacyClicked is called`() {
        interactor.onSponsorPrivacyClicked()
        verify { controller.handleSponsorPrivacyClicked() }
    }
}
