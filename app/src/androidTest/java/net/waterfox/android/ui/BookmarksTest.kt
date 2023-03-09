/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import mozilla.appservices.places.BookmarkRoot
import net.waterfox.android.customannotations.SmokeTest
import net.waterfox.android.ext.bookmarkStorage
import net.waterfox.android.ext.settings
import net.waterfox.android.helpers.*
import net.waterfox.android.ui.robots.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 *  Tests for verifying basic functionality of bookmarks
 */
class BookmarksTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mDevice: UiDevice
    private val bookmarksFolderName = "New Folder"
    private val testBookmark = object {
        var title: String = "Bookmark title"
        var url: String = "https://www.test.com/"
    }

    @get:Rule(order = 0)
    val composeTestRule = AndroidComposeTestRule(
        HomeActivityIntentTestRule()
    ) { it.activity }

    private val activity by lazy { composeTestRule.activity }

    @Rule(order = 1)
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }

        val settings = activity.settings()
        settings.shouldShowJumpBackInCFR = false
        settings.shouldShowTotalCookieProtectionCFR = false
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        // Clearing all bookmarks data after each test to avoid overlapping data
        val bookmarksStorage = activity.bookmarkStorage
        runBlocking {
            val bookmarks = bookmarksStorage.getTree(BookmarkRoot.Mobile.id)?.children
            bookmarks?.forEach { bookmarksStorage.deleteNode(it.guid) }
        }
    }

    @Test
    fun verifyEmptyBookmarksMenuTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            verifyBookmarksMenuView()
            verifyAddFolderButton()
            verifyCloseButton()
            verifyBookmarkTitle("Desktop Bookmarks", composeTestRule)
        }
    }

    @Test
    fun verifyEmptyBookmarksListTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            clickAddFolderButton()
            verifyKeyboardVisible()
            addNewFolderName("Empty", composeTestRule)
            saveNewFolder()
            selectFolder("Empty", composeTestRule)
            verifyEmptyFolder(composeTestRule)
        }
    }

    @Test
    fun defaultDesktopBookmarksFoldersTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            selectFolder("Desktop Bookmarks", composeTestRule)
            verifyFolderTitle("Bookmarks Menu", composeTestRule)
            verifyFolderTitle("Bookmarks Toolbar", composeTestRule)
            verifyFolderTitle("Other Bookmarks", composeTestRule)
            verifySignInToSyncButton(composeTestRule)
        }.clickSingInToSyncButton(composeTestRule) {
            verifyTurnOnSyncToolbarTitle()
        }
    }

    @Test
    fun verifyBookmarkButtonTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.bookmarkPage {
        }.openThreeDotMenu {
            verifyEditBookmarkButton()
        }
    }

    @Test
    fun addBookmarkTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
            verifyBookmarkedUrl(defaultWebPage.url.toString(), composeTestRule)
            verifyBookmarkFavicon(defaultWebPage.url, composeTestRule)
        }
    }

    @Test
    fun createBookmarkFolderTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            clickAddFolderButton()
            verifyKeyboardVisible()
            addNewFolderName(bookmarksFolderName, composeTestRule)
            saveNewFolder()
            verifyFolderTitle(bookmarksFolderName, composeTestRule)
            verifyKeyboardHidden()
        }
    }

    @Test
    fun addBookmarkThenCreateFolderTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
            clickAddFolderButton()
            verifyKeyboardVisible()
            addNewFolderName(bookmarksFolderName, composeTestRule)
            saveNewFolder()
            verifyBookmarkItemPosition("Desktop Bookmarks", 0, composeTestRule)
            verifyBookmarkItemPosition(bookmarksFolderName, 1, composeTestRule)
            verifyBookmarkItemPosition(defaultWebPage.title, 2, composeTestRule)
        }
    }

    @Test
    fun cancelCreateBookmarkFolderTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            clickAddFolderButton()
            addNewFolderName(bookmarksFolderName, composeTestRule)
            navigateUp()
            verifyKeyboardHidden()
            verifyBookmarkFolderIsNotCreated(bookmarksFolderName, composeTestRule)
        }
    }

    @Test
    fun threeDotMenuItemsForFolderTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            createFolder("1", composeTestRule)
        }.openThreeDotMenu("1", composeTestRule) {
            verifyEditButton(composeTestRule)
            verifyDeleteButton(composeTestRule)
        }
    }

    @Test
    fun threeDotMenuItemsForSiteTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
        }.openThreeDotMenu(defaultWebPage.url, composeTestRule) {
            verifyEditButton(composeTestRule)
            verifyCopyButton(composeTestRule)
            verifyShareButton(composeTestRule)
            verifyOpenInNewTabButton(composeTestRule)
            verifyOpenInPrivateTabButton(composeTestRule)
            verifyDeleteButton(composeTestRule)
        }
    }

    @SmokeTest
    @Test
    fun editBookmarkTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
        }.openThreeDotMenu(defaultWebPage.url, composeTestRule) {
        }.clickEdit(composeTestRule) {
            verifyEditBookmarksView()
            verifyBookmarkNameEditBox(composeTestRule)
            verifyBookmarkUrlEditBox(composeTestRule)
            verifyParentFolderSelector(composeTestRule)
            changeBookmarkTitle(testBookmark.title, composeTestRule)
            changeBookmarkUrl(testBookmark.url, composeTestRule)
            saveEditBookmark()
            verifyBookmarkTitle(testBookmark.title, composeTestRule)
            verifyBookmarkedUrl(testBookmark.url, composeTestRule)
            verifyKeyboardHidden()
        }
    }

    @Test
    fun copyBookmarkUrlTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
        }.openThreeDotMenu(defaultWebPage.url, composeTestRule) {
        }.clickCopy(composeTestRule) {
            verifyCopySnackBarText()
            navigateUp()
        }

        navigationToolbar {
        }.clickUrlbar {
            clickClearButton()
            longClickToolbar()
            clickPasteText()
            verifyPastedToolbarText(defaultWebPage.url.toString())
        }
    }

    @Test
    fun threeDotMenuShareBookmarkTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
        }.openThreeDotMenu(defaultWebPage.url, composeTestRule) {
        }.clickShare(composeTestRule) {
            verifyShareOverlay()
            verifyShareBookmarkFavicon()
            verifyShareBookmarkTitle()
            verifyShareBookmarkUrl()
        }
    }

    @Test
    fun openBookmarkInNewTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
        }.openThreeDotMenu(defaultWebPage.url, composeTestRule) {
        }.clickOpenInNewTab(composeTestRule) {
            verifyTabTrayIsOpened()
            verifyNormalModeSelected()
        }
    }

    @Test
    fun openBookmarkInPrivateTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
        }.openThreeDotMenu(defaultWebPage.url, composeTestRule) {
        }.clickOpenInPrivateTab(composeTestRule) {
            verifyTabTrayIsOpened()
            verifyPrivateModeSelected()
        }
    }

    @SmokeTest
    @Test
    fun deleteBookmarkTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
        }.openThreeDotMenu(defaultWebPage.url, composeTestRule) {
            verifyDeleteButtonStyle(composeTestRule)
        }.clickDelete(composeTestRule) {
            verifyDeleteSnackBarText()
            verifyUndoDeleteSnackBarButton()
        }
    }

    @SmokeTest
    @Test
    fun undoDeleteBookmarkTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
        }.openThreeDotMenu(defaultWebPage.url, composeTestRule) {
        }.clickDelete(composeTestRule) {
            verifyUndoDeleteSnackBarButton()
            clickUndoDeleteButton()
            verifySnackBarHidden()
            verifyBookmarkedUrl(defaultWebPage.url.toString(), composeTestRule)
        }
    }

    @SmokeTest
    @Test
    fun bookmarksMultiSelectionToolbarItemsTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
            longTapSelectItem(defaultWebPage.url, composeTestRule)
        }

        multipleSelectionToolbar {
            verifyMultiSelectionCheckmark(defaultWebPage.url, composeTestRule)
            verifyMultiSelectionCounter()
            verifyShareBookmarksButton()
            verifyCloseToolbarButton()
        }.closeToolbarReturnToBookmarks {
            verifyBookmarksMenuView()
        }
    }

    @SmokeTest
    @Test
    fun openSelectionInNewTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openTabDrawer {
            closeTab()
        }

        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            longTapSelectItem(defaultWebPage.url, composeTestRule)
            openActionBarOverflowOrOptionsMenu(activity)
        }

        multipleSelectionToolbar {
        }.clickOpenNewTab {
            verifyNormalModeSelected()
            verifyExistingTabList()
        }
    }

    @SmokeTest
    @Test
    fun openSelectionInPrivateTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
            longTapSelectItem(defaultWebPage.url, composeTestRule)
            openActionBarOverflowOrOptionsMenu(activity)
        }

        multipleSelectionToolbar {
        }.clickOpenPrivateTab {
            verifyPrivateModeSelected()
            verifyExistingTabList()
        }
    }

    @SmokeTest
    @Test
    fun deleteMultipleSelectionTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        browserScreen {
            createBookmark(firstWebPage.url)
            createBookmark(secondWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
            longTapSelectItem(firstWebPage.url, composeTestRule)
            tapSelectItem(secondWebPage.url, composeTestRule)
            openActionBarOverflowOrOptionsMenu(activity)
        }

        multipleSelectionToolbar {
            clickMultiSelectionDelete()
        }

        bookmarksMenu {
            verifyDeleteMultipleBookmarksSnackBar()
        }
    }

    @SmokeTest
    @Test
    fun undoDeleteMultipleSelectionTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        browserScreen {
            createBookmark(firstWebPage.url)
            createBookmark(secondWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
            longTapSelectItem(firstWebPage.url, composeTestRule)
            tapSelectItem(secondWebPage.url, composeTestRule)
            openActionBarOverflowOrOptionsMenu(activity)
        }

        multipleSelectionToolbar {
            clickMultiSelectionDelete()
        }

        bookmarksMenu {
            verifyDeleteMultipleBookmarksSnackBar()
            clickUndoDeleteButton()
            verifyBookmarkedUrl(firstWebPage.url.toString(), composeTestRule)
            verifyBookmarkedUrl(secondWebPage.url.toString(), composeTestRule)
        }
    }

    @Test
    fun multipleSelectionShareButtonTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
            longTapSelectItem(defaultWebPage.url, composeTestRule)
        }

        multipleSelectionToolbar {
            clickShareBookmarksButton()
            verifyShareOverlay()
            verifyShareTabFavicon()
            verifyShareTabTitle()
            verifyShareTabUrl()
        }
    }

    @Test
    fun multipleBookmarkDeletionsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            createFolder("1", composeTestRule)
            createFolder("2", composeTestRule)
            createFolder("3", composeTestRule)
        }.openThreeDotMenu("1", composeTestRule) {
        }.clickDelete(composeTestRule) {
            verifyDeleteFolderConfirmationMessage()
            confirmDeletion()
            verifyDeleteSnackBarText()
        }.openThreeDotMenu("2", composeTestRule) {
        }.clickDelete(composeTestRule) {
            verifyDeleteFolderConfirmationMessage()
            confirmDeletion()
            verifyDeleteSnackBarText()
            verifyFolderTitle("3", composeTestRule)
            // On some devices we need to wait for the Snackbar to be gone before continuing
            TestHelper.waitUntilSnackbarGone()
        }.closeMenu {
        }

        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            verifyFolderTitle("3", composeTestRule)
        }
    }

    @SmokeTest
    @Test
    fun changeBookmarkParentFolderTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
            createFolder(bookmarksFolderName, composeTestRule)
        }.openThreeDotMenu(defaultWebPage.title, composeTestRule) {
        }.clickEdit(composeTestRule) {
            clickParentFolderSelector(composeTestRule)
            selectFolder(bookmarksFolderName, composeTestRule)
            navigateUp()
            saveEditBookmark()
            selectFolder(bookmarksFolderName, composeTestRule)
            verifyBookmarkedUrl(defaultWebPage.url.toString(), composeTestRule)
        }
    }

    @Test
    fun navigateBookmarksFoldersTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            createFolder("1", composeTestRule)
            waitForBookmarksFolderContentToExist("Bookmarks", "1")
            selectFolder("1", composeTestRule)
            verifyCurrentFolderTitle("1")
            createFolder("2", composeTestRule)
            waitForBookmarksFolderContentToExist("1", "2")
            selectFolder("2", composeTestRule)
            verifyCurrentFolderTitle("2")
            navigateUp()
            waitForBookmarksFolderContentToExist("1", "2")
            verifyCurrentFolderTitle("1")
            mDevice.pressBack()
            verifyBookmarksMenuView()
        }
    }

    @Test
    fun cantSelectDesktopFoldersTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            longTapDesktopFolder(composeTestRule)
            verifySelectDefaultFolderSnackBarText()
        }
    }

    @Test
    fun verifyCloseMenuTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
        }.closeMenu {
            verifyHomeScreen()
        }
    }

    @Test
    fun deleteBookmarkInEditModeTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openBookmarks {
        }.openThreeDotMenu(defaultWebPage.url, composeTestRule) {
        }.clickEdit(composeTestRule) {
            clickDeleteInEditModeButton()
            cancelDeletion()
            clickDeleteInEditModeButton()
            confirmDeletion()
            verifyDeleteSnackBarText()
            verifyBookmarkIsDeleted("Test_Page_1", composeTestRule)
        }
    }

    @SmokeTest
    @Test
    fun undoDeleteBookmarkFolderTest() {
        browserScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            createFolder("My Folder", composeTestRule)
            verifyFolderTitle("My Folder", composeTestRule)
        }.openThreeDotMenu("My Folder", composeTestRule) {
        }.clickDelete(composeTestRule) {
            cancelFolderDeletion()
            verifyFolderTitle("My Folder", composeTestRule)
        }.openThreeDotMenu("My Folder", composeTestRule) {
        }.clickDelete(composeTestRule) {
            confirmDeletion()
            verifyDeleteSnackBarText()
            clickUndoDeleteButton()
            verifyFolderTitle("My Folder", composeTestRule)
        }
    }
}
