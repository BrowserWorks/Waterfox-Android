/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package net.waterfox.android.ui.robots

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import net.waterfox.android.R
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.TestHelper.getStringResource
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.TestHelper.packageName
import net.waterfox.android.helpers.click
import net.waterfox.android.helpers.ext.waitNotNull
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals

/**
 * Implementation of Robot Pattern for the bookmarks menu.
 */
class BookmarksRobot {

    fun verifyBookmarksMenuView() {
        mDevice.findObject(
            UiSelector().text("Bookmarks"),
        ).waitForExists(waitingTime)

        assertBookmarksView()
    }

    fun verifyAddFolderButton() = assertAddFolderButton()

    fun verifyCloseButton() = assertCloseButton()

    fun verifyDeleteMultipleBookmarksSnackBar() = assertSnackBarText("Bookmarks deleted")

    fun verifyEmptyFolder(rule: ComposeTestRule) = assertEmptyBookmarksView(rule)

    fun verifyBookmarkFavicon(forUrl: Uri, rule: ComposeTestRule) =
        assertBookmarkFavicon(forUrl, rule)

    fun verifyBookmarkedUrl(url: String, rule: ComposeTestRule) = assertBookmarkUrl(url, rule)

    fun verifyFolderTitle(title: String, rule: ComposeTestRule) = assertFolderTitle(title, rule)

    fun verifyBookmarkFolderIsNotCreated(title: String, rule: ComposeTestRule) =
        assertBookmarkFolderIsNotCreated(title, rule)

    fun verifyBookmarkTitle(title: String, rule: ComposeTestRule) = assertBookmarkTitle(title, rule)

    fun verifyBookmarkIsDeleted(expectedTitle: String, rule: ComposeTestRule) =
        assertBookmarkIsDeleted(expectedTitle, rule)

    fun verifyDeleteSnackBarText() = assertSnackBarText("Deleted")

    fun verifyUndoDeleteSnackBarButton() = assertUndoDeleteSnackBarButton()

    fun verifySnackBarHidden() {
        mDevice.waitNotNull(
            Until.gone(By.text("UNDO")),
            waitingTime,
        )
        onView(withId(R.id.snackbar_layout)).check(doesNotExist())
    }

    fun verifyCopySnackBarText() = assertSnackBarText("URL copied")

    fun verifyEditBookmarksView() = assertEditBookmarksView()

    fun verifyBookmarkNameEditBox(rule: ComposeTestRule) =
        assertBookmarkNameEditBox(rule)

    fun verifyBookmarkUrlEditBox(rule: ComposeTestRule) =
        assertBookmarkUrlEditBox(rule)

    fun verifyParentFolderSelector(rule: ComposeTestRule) =
        assertBookmarkFolderSelector(rule)

    fun verifyKeyboardHidden() = assertKeyboardVisibility(isExpectedToBeVisible = false)

    fun verifyKeyboardVisible() = assertKeyboardVisibility(isExpectedToBeVisible = true)

    fun verifyBookmarkItemPosition(title: String, position: Int, rule: ComposeTestRule) =
        assertBookmarkItemPosition(title, position, rule)

    fun verifyShareOverlay() = assertShareOverlay()

    fun verifyShareBookmarkFavicon() = assertShareBookmarkFavicon()

    fun verifyShareBookmarkTitle() = assertShareBookmarkTitle()

    fun verifyShareBookmarkUrl() = assertShareBookmarkUrl()

    fun verifySelectDefaultFolderSnackBarText() = assertSnackBarText("Can’t edit default folders")

    fun verifyCurrentFolderTitle(title: String) {
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/navigationToolbar")
                .textContains(title),
        )
            .waitForExists(waitingTime)

        onView(
            allOf(
                withText(title),
                withParent(withId(R.id.navigationToolbar)),
            ),
        )
            .check(matches(isDisplayed()))
    }

    fun waitForBookmarksFolderContentToExist(parentFolderName: String, childFolderName: String) {
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/navigationToolbar")
                .textContains(parentFolderName),
        )
            .waitForExists(waitingTime)

        mDevice.waitNotNull(Until.findObject(By.text(childFolderName)), waitingTime)
    }

    fun verifySignInToSyncButton(rule: ComposeTestRule) =
        signInToSyncButton(rule).assertIsDisplayed()

    fun verifyDeleteFolderConfirmationMessage() = assertDeleteFolderConfirmationMessage()

    fun cancelFolderDeletion() {
        onView(withText("CANCEL"))
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
            .click()
    }

    fun createFolder(name: String, rule: ComposeTestRule) {
        clickAddFolderButton()
        addNewFolderName(name, rule)
        saveNewFolder()
    }

    fun clickAddFolderButton() {
        mDevice.waitNotNull(
            Until.findObject(By.desc("Add folder")),
            waitingTime,
        )
        addFolderButton().click()
    }

    fun addNewFolderName(name: String, rule: ComposeTestRule) =
        addFolderTitleField(rule).performTextInput(name)

    fun saveNewFolder() {
        saveFolderButton().click()
    }

    fun navigateUp() {
        goBackButton().click()
    }

    fun clickUndoDeleteButton() {
        snackBarUndoButton().click()
    }

    fun changeBookmarkTitle(newTitle: String, rule: ComposeTestRule) {
        bookmarkNameEditBox(rule).performTextReplacement(newTitle)
    }

    fun changeBookmarkUrl(newUrl: String, rule: ComposeTestRule) {
        bookmarkUrlEditBox(rule).performTextReplacement(newUrl)
    }

    fun saveEditBookmark() {
        saveBookmarkButton().click()
    }

    fun clickParentFolderSelector(rule: ComposeTestRule) =
        bookmarkFolderSelector(rule).performClick()

    fun selectFolder(title: String, rule: ComposeTestRule) =
        rule.onNodeWithText(title).performClick()

    fun longTapDesktopFolder(rule: ComposeTestRule) =
        rule.onNodeWithText("Desktop Bookmarks")
            .performTouchInput { longClick() }

    fun longTapSelectItem(url: Uri, rule: ComposeTestRule) =
        rule.onAllNodesWithTag("library.site.item.url", useUnmergedTree = true)
            .filterToOne(hasText(url.toString()))
            .performTouchInput { longClick() }

    fun tapSelectItem(url: Uri, rule: ComposeTestRule) =
        rule.onAllNodesWithTag("library.site.item.url", useUnmergedTree = true)
            .filterToOne(hasText(url.toString()))
            .performClick()

    fun cancelDeletion() {
        val cancelButton = mDevice.findObject(UiSelector().textContains("CANCEL"))
        cancelButton.waitForExists(waitingTime)
        cancelButton.click()
    }

    fun confirmDeletion() {
        onView(withText(R.string.delete_browsing_data_prompt_allow))
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
            .click()
    }

    fun clickDeleteInEditModeButton() = deleteInEditModeButton().click()

    class Transition {
        fun closeMenu(interact: HomeScreenRobot.() -> Unit): Transition {
            closeButton().click()

            HomeScreenRobot().interact()
            return Transition()
        }

        fun openThreeDotMenu(
            bookmarkTitle: String,
            rule: ComposeTestRule,
            interact: ThreeDotMenuBookmarksRobot.() -> Unit,
        ): ThreeDotMenuBookmarksRobot.Transition {
            threeDotMenu(bookmarkTitle, rule).performClick()

            ThreeDotMenuBookmarksRobot().interact()
            return ThreeDotMenuBookmarksRobot.Transition()
        }

        fun openThreeDotMenu(
            bookmarkUrl: Uri,
            rule: ComposeTestRule,
            interact: ThreeDotMenuBookmarksRobot.() -> Unit,
        ): ThreeDotMenuBookmarksRobot.Transition {
            threeDotMenu(bookmarkUrl, rule).performClick()

            ThreeDotMenuBookmarksRobot().interact()
            return ThreeDotMenuBookmarksRobot.Transition()
        }

        fun clickSingInToSyncButton(
            rule: ComposeTestRule,
            interact: SettingsTurnOnSyncRobot.() -> Unit,
        ): SettingsTurnOnSyncRobot.Transition {
            signInToSyncButton(rule).performClick()

            SettingsTurnOnSyncRobot().interact()
            return SettingsTurnOnSyncRobot.Transition()
        }
    }
}

fun bookmarksMenu(interact: BookmarksRobot.() -> Unit): BookmarksRobot.Transition {
    BookmarksRobot().interact()
    return BookmarksRobot.Transition()
}

private fun closeButton() = onView(withId(R.id.close_bookmarks))

private fun goBackButton() = onView(withContentDescription("Navigate up"))

private fun addFolderButton() = onView(withId(R.id.add_bookmark_folder))

private fun addFolderTitleField(rule: ComposeTestRule) = bookmarkNameEditBox(rule)

private fun saveFolderButton() = onView(withId(R.id.confirm_add_folder_button))

private fun threeDotMenu(bookmarkUrl: Uri, rule: ComposeTestRule) = rule
    .onAllNodesWithTag("library.site.item.overflow.menu")
    .filterToOne(hasAnyAncestor(hasText(bookmarkUrl.toString())))

private fun threeDotMenu(bookmarkTitle: String, rule: ComposeTestRule) = rule
    .onAllNodesWithTag("library.site.item.overflow.menu")
    .filterToOne(hasAnyAncestor(hasText(bookmarkTitle)))

private fun snackBarText() = onView(withId(R.id.snackbar_text))

private fun snackBarUndoButton() = onView(withId(R.id.snackbar_btn))

private fun bookmarkNameEditBox(rule: ComposeTestRule) =
    rule.onNodeWithTag("edit.bookmark.name")

private fun bookmarkUrlEditBox(rule: ComposeTestRule) =
    rule.onNodeWithTag("edit.bookmark.url")

private fun bookmarkFolderSelector(rule: ComposeTestRule) =
    rule.onNodeWithTag("edit.bookmark.parent.folder")

private fun saveBookmarkButton() = onView(withId(R.id.save_bookmark_button))

private fun deleteInEditModeButton() = onView(withId(R.id.delete_bookmark_button))

private fun signInToSyncButton(rule: ComposeTestRule) =
    rule.onNodeWithText(getStringResource(R.string.bookmark_sign_in_button))

private fun assertBookmarksView() {
    onView(
        allOf(
            withText("Bookmarks"),
            withParent(withId(R.id.navigationToolbar)),
        ),
    )
        .check(matches(isDisplayed()))
}

private fun assertAddFolderButton() =
    addFolderButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertCloseButton() =
    closeButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertEmptyBookmarksView(rule: ComposeTestRule) =
    rule.onNodeWithText("No bookmarks here").assertIsDisplayed()

private fun assertBookmarkFavicon(forUrl: Uri, rule: ComposeTestRule) =
    rule.onAllNodesWithTag("library.site.item.favicon", useUnmergedTree = true)
        .filterToOne(hasAnyAncestor(hasAnyChild(hasText(forUrl.toString()))))
        .assertIsDisplayed()

private fun assertBookmarkUrl(expectedUrl: String, rule: ComposeTestRule) =
    rule.onAllNodesWithTag("library.site.item.url", useUnmergedTree = true)
        .filterToOne(hasText(expectedUrl))
        .assertIsDisplayed()

private fun assertFolderTitle(expectedTitle: String, rule: ComposeTestRule) =
    rule.onNodeWithText(expectedTitle).assertIsDisplayed()

private fun assertBookmarkTitle(expectedTitle: String, rule: ComposeTestRule) =
    rule.onNodeWithText(expectedTitle).assertIsDisplayed()

private fun assertBookmarkFolderIsNotCreated(title: String, rule: ComposeTestRule) =
    rule.onNodeWithText(title).assertDoesNotExist()

private fun assertBookmarkIsDeleted(expectedTitle: String, rule: ComposeTestRule) =
    rule.onNode(hasTestTag("library.site.item.title") and hasText(expectedTitle))
        .assertDoesNotExist()

private fun assertUndoDeleteSnackBarButton() =
    snackBarUndoButton().check(matches(withText("UNDO")))

private fun assertSnackBarText(text: String) =
    snackBarText().check(matches(withText(containsString(text))))

private fun assertEditBookmarksView() = onView(withText("Edit bookmark"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertBookmarkNameEditBox(rule: ComposeTestRule) =
    bookmarkNameEditBox(rule).assertIsDisplayed()

private fun assertBookmarkUrlEditBox(rule: ComposeTestRule) =
    bookmarkUrlEditBox(rule).assertIsDisplayed()

private fun assertBookmarkFolderSelector(rule: ComposeTestRule) =
    bookmarkFolderSelector(rule).assertIsDisplayed()

private fun assertKeyboardVisibility(isExpectedToBeVisible: Boolean) =
    assertEquals(
        isExpectedToBeVisible,
        mDevice
            .executeShellCommand("dumpsys input_method | grep mInputShown")
            .contains("mInputShown=true"),
    )

private fun assertBookmarkItemPosition(expectedTitle: String, expectedPosition: Int, rule: ComposeTestRule) =
    rule.onNodeWithTag("bookmark.list")
        .onChildAt(expectedPosition)
        .assertTextContains(expectedTitle)

private fun assertShareOverlay() =
    onView(withId(R.id.shareWrapper)).check(matches(isDisplayed()))

private fun assertShareBookmarkTitle() =
    onView(withId(R.id.share_tab_title)).check(matches(isDisplayed()))

private fun assertShareBookmarkFavicon() =
    onView(withId(R.id.share_tab_favicon)).check(matches(isDisplayed()))

private fun assertShareBookmarkUrl() =
    onView(withId(R.id.share_tab_url)).check(matches(isDisplayed()))

private fun assertDeleteFolderConfirmationMessage() =
    onView(withText(R.string.bookmark_delete_folder_confirmation_dialog))
        .inRoot(RootMatchers.isDialog())
        .check(matches(isDisplayed()))
