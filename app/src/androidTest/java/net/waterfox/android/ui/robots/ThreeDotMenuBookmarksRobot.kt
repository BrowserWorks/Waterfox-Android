/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package net.waterfox.android.ui.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import mozilla.components.ui.colors.PhotonColors
import net.waterfox.android.helpers.TestHelper.assertTextColor

/**
 * Implementation of Robot Pattern for the Bookmarks three dot menu.
 */
class ThreeDotMenuBookmarksRobot {

    fun verifyEditButton(rule: ComposeTestRule) = editButton(rule).assertIsDisplayed()

    fun verifyCopyButton(rule: ComposeTestRule) = copyButton(rule).assertIsDisplayed()

    fun verifyShareButton(rule: ComposeTestRule) = shareButton(rule).assertIsDisplayed()

    fun verifyOpenInNewTabButton(rule: ComposeTestRule) =
        openInNewTabButton(rule).assertIsDisplayed()

    fun verifyOpenInPrivateTabButton(rule: ComposeTestRule) =
        openInPrivateTabButton(rule).assertIsDisplayed()

    fun verifyDeleteButton(rule: ComposeTestRule) = deleteButton(rule).assertIsDisplayed()

    fun verifyDeleteButtonStyle(rule: ComposeTestRule) =
        deleteButton(rule)
            .assertIsDisplayed()
            .assertTextColor(PhotonColors.Red70)

    class Transition {

        fun clickEdit(rule: ComposeTestRule, interact: BookmarksRobot.() -> Unit): BookmarksRobot.Transition {
            editButton(rule).performClick()

            BookmarksRobot().interact()
            return BookmarksRobot.Transition()
        }

        fun clickCopy(rule: ComposeTestRule, interact: BookmarksRobot.() -> Unit): BookmarksRobot.Transition {
            copyButton(rule).performClick()

            BookmarksRobot().interact()
            return BookmarksRobot.Transition()
        }

        fun clickShare(rule: ComposeTestRule, interact: BookmarksRobot.() -> Unit): BookmarksRobot.Transition {
            shareButton(rule).performClick()

            BookmarksRobot().interact()
            return BookmarksRobot.Transition()
        }

        fun clickOpenInNewTab(rule: ComposeTestRule, interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            openInNewTabButton(rule).performClick()

            TabDrawerRobot().interact()
            return TabDrawerRobot.Transition()
        }

        fun clickOpenInPrivateTab(rule: ComposeTestRule, interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            openInPrivateTabButton(rule).performClick()

            TabDrawerRobot().interact()
            return TabDrawerRobot.Transition()
        }

        fun clickDelete(rule: ComposeTestRule, interact: BookmarksRobot.() -> Unit): BookmarksRobot.Transition {
            deleteButton(rule).performClick()

            BookmarksRobot().interact()
            return BookmarksRobot.Transition()
        }
    }
}

private fun editButton(rule: ComposeTestRule) = rule.onNodeWithText("Edit")

private fun copyButton(rule: ComposeTestRule) = rule.onNodeWithText("Copy")

private fun shareButton(rule: ComposeTestRule) = rule.onNodeWithText("Share")

private fun openInNewTabButton(rule: ComposeTestRule) = rule.onNodeWithText("Open in new tab")

private fun openInPrivateTabButton(rule: ComposeTestRule) = rule.onNodeWithText("Open in private tab")

private fun deleteButton(rule: ComposeTestRule) = rule.onNodeWithText("Delete")
