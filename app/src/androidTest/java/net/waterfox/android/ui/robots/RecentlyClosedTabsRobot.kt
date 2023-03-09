/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui.robots

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import net.waterfox.android.R
import net.waterfox.android.helpers.TestHelper.getStringResource
import net.waterfox.android.helpers.TestHelper.mDevice
import org.hamcrest.Matchers.allOf

/**
 * Implementation of Robot Pattern for the recently closed tabs menu.
 */

class RecentlyClosedTabsRobot {

    fun verifyRecentlyClosedTabsMenuView() = assertRecentlyClosedTabsMenuView()

    fun verifyEmptyRecentlyClosedTabsList(rule: ComposeTestRule) =
        assertEmptyRecentlyClosedTabsList(rule)

    fun verifyRecentlyClosedTabsPageTitle(title: String, rule: ComposeTestRule) =
        assertRecentlyClosedTabsPageTitle(title, rule)

    fun verifyRecentlyClosedTabsUrl(expectedUrl: Uri, rule: ComposeTestRule) =
        assertPageUrl(expectedUrl, rule)

    fun clickDeleteRecentlyClosedTabs(rule: ComposeTestRule) =
        recentlyClosedTabsDeleteButton(rule).performClick()

    class Transition {
        fun clickRecentlyClosedItem(
            title: String,
            rule: ComposeTestRule,
            interact: BrowserRobot.() -> Unit,
        ): BrowserRobot.Transition {
            recentlyClosedTabsPageTitle(title, rule).performClick()
            mDevice.waitForIdle()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

private fun assertRecentlyClosedTabsMenuView() {
    onView(
        allOf(
            withText("Recently closed tabs"),
            withParent(withId(R.id.navigationToolbar)),
        ),
    )
        .check(
            matches(withEffectiveVisibility(Visibility.VISIBLE)),
        )
}

private fun assertEmptyRecentlyClosedTabsList(rule: ComposeTestRule) =
    rule.onNodeWithText(getStringResource(R.string.recently_closed_empty_message))
        .assertIsDisplayed()

private fun assertPageUrl(expectedUrl: Uri, rule: ComposeTestRule) = rule.onNode(
    hasTestTag("library.site.item.url") and hasText(expectedUrl.toString()),
    useUnmergedTree = true,
).assertIsDisplayed()

private fun recentlyClosedTabsPageTitle(title: String, rule: ComposeTestRule) =
    rule.onNode(
        hasTestTag("library.site.item.title") and hasText(title),
        useUnmergedTree = true,
    )

private fun assertRecentlyClosedTabsPageTitle(title: String, rule: ComposeTestRule) =
    recentlyClosedTabsPageTitle(title, rule).assertIsDisplayed()

private fun recentlyClosedTabsDeleteButton(rule: ComposeTestRule) =
    rule.onNodeWithTag("library.site.item.trailing.icon")
