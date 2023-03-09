/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui.robots

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.uiautomator.UiSelector
import net.waterfox.android.R
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.TestHelper.getStringResource
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.TestHelper.packageName
import net.waterfox.android.helpers.click
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Implementation of Robot Pattern for the history menu.
 */
class HistoryRobot {

    fun verifyHistoryMenuView() = assertHistoryMenuView()

    fun verifyEmptyHistoryView(rule: ComposeTestRule) = assertEmptyHistoryView(rule)

    fun verifyHistoryListExists(rule: ComposeTestRule) = assertHistoryListExists(rule)

    fun verifyVisitedTimeTitle(rule: ComposeTestRule) = assertVisitedTimeTitle(rule)

    fun verifyHistoryItemExists(shouldExist: Boolean, item: String, rule: ComposeTestRule) =
        assertHistoryItemExists(shouldExist, item, rule)

    fun verifyFirstTestPageTitle(rule: ComposeTestRule) = assertTestPageTitle(rule)

    fun verifyTestPageUrl(expectedUrl: Uri, rule: ComposeTestRule) = assertPageUrl(expectedUrl, rule)

    fun verifyCopySnackBarText() = assertCopySnackBarText()

    fun verifyDeleteConfirmationMessage() = assertDeleteConfirmationMessage()

    fun verifyHomeScreen() = HomeScreenRobot().verifyHomeScreen()

    fun clickDeleteHistoryButton(item: String, rule: ComposeTestRule) {
        deleteButton(item, rule).performClick()
    }

    fun clickDeleteAllHistoryButton() = deleteButton().click()

    fun selectEverythingOption() = deleteHistoryEverythingOption().click()

    fun confirmDeleteAllHistory() {
        onView(withText("Delete"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
            .click()
    }

    fun cancelDeleteHistory() =
        mDevice
            .findObject(
                UiSelector()
                    .textContains(getStringResource(R.string.delete_browsing_data_prompt_cancel)),
            ).click()

    fun verifyDeleteSnackbarText(text: String) = assertSnackBarText(text)

    fun verifyUndoDeleteSnackBarButton() = assertUndoDeleteSnackBarButton()

    fun clickUndoDeleteButton() {
        snackBarUndoButton().click()
    }

    fun longTapSelectItem(url: Uri, rule: ComposeTestRule) =
        rule.onAllNodesWithTag("library.site.item.url", useUnmergedTree = true)
            .filterToOne(hasText(url.toString()))
            .performTouchInput { longClick() }

    fun tapSelectItem(url: Uri, rule: ComposeTestRule) =
        rule.onAllNodesWithTag("library.site.item.url", useUnmergedTree = true)
            .filterToOne(hasText(url.toString()))
            .performClick()

    class Transition {
        fun goBackToBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.pressBack()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

fun historyMenu(interact: HistoryRobot.() -> Unit): HistoryRobot.Transition {
    HistoryRobot().interact()
    return HistoryRobot.Transition()
}

private fun deleteButton(title: String, rule: ComposeTestRule) =
    rule.onAllNodesWithTag("library.site.item.trailing.icon")
        .filterToOne(hasAnyAncestor(hasText(title)))

private fun deleteButton() = onView(withId(R.id.history_delete))

private fun snackBarText() = onView(withId(R.id.snackbar_text))

private fun assertHistoryMenuView() {
    onView(
        allOf(withText("History"), withParent(withId(R.id.navigationToolbar))),
    )
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertEmptyHistoryView(rule: ComposeTestRule) =
    rule.onNodeWithText("No history here").assertIsDisplayed()

private fun assertHistoryListExists(rule: ComposeTestRule) =
    rule.onNodeWithTag("history.list").assertExists()

private fun assertHistoryItemExists(shouldExist: Boolean, item: String, rule: ComposeTestRule) {
    if (shouldExist) {
        rule.onNodeWithText(item).assertIsDisplayed()
    } else {
        rule.onNodeWithText(item).assertIsNotDisplayed()
    }
}

private fun assertVisitedTimeTitle(rule: ComposeTestRule) =
    rule.onNodeWithText("Today").assertIsDisplayed()

private fun assertTestPageTitle(rule: ComposeTestRule) = rule.onNode(
    hasTestTag("library.site.item.title") and hasText("Test_Page_1"),
    useUnmergedTree = true,
).assertIsDisplayed()

private fun assertPageUrl(expectedUrl: Uri, rule: ComposeTestRule) = rule.onNode(
    hasTestTag("library.site.item.url") and hasText(expectedUrl.toString()),
    useUnmergedTree = true
).assertIsDisplayed()

private fun assertDeleteConfirmationMessage() {
    assertTrue(deleteHistoryPromptTitle().waitForExists(waitingTime))
    assertTrue(deleteHistoryPromptSummary().waitForExists(waitingTime))
}

private fun assertCopySnackBarText() = snackBarText().check(matches(withText("URL copied")))

private fun assertSnackBarText(text: String) =
    snackBarText().check(matches(withText(Matchers.containsString(text))))

private fun snackBarUndoButton() = onView(withId(R.id.snackbar_btn))

private fun assertUndoDeleteSnackBarButton() =
    snackBarUndoButton().check(matches(withText("UNDO")))

private fun deleteHistoryPromptTitle() =
    mDevice
        .findObject(
            UiSelector()
                .textContains(getStringResource(R.string.delete_history_prompt_title))
                .resourceId("$packageName:id/title"),
        )

private fun deleteHistoryPromptSummary() =
    mDevice
        .findObject(
            UiSelector()
                .textContains(getStringResource(R.string.delete_history_prompt_body))
                .resourceId("$packageName:id/body"),
        )

private fun deleteHistoryEverythingOption() =
    mDevice
        .findObject(
            UiSelector()
                .textContains(getStringResource(R.string.delete_history_prompt_button_everything))
                .resourceId("$packageName:id/everything_button"),
        )
