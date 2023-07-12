/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui.robots

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiSelector
import junit.framework.TestCase.assertTrue
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.TestHelper.mDevice
import org.hamcrest.CoreMatchers

class SettingsSubMenuLanguageRobot {
    fun selectLanguage(language: String, rule: ComposeTestRule) {
        languagesList(rule).performScrollToNode(hasText(language))

        rule.onNodeWithText(language)
            .assertIsDisplayed()
            .performClick()
    }

    fun verifyLanguageList(rule: ComposeTestRule) =
        rule.onNodeWithTag("locale.list").assertIsDisplayed()

    fun verifyLanguageListItem(language: String, selected: Boolean, rule: ComposeTestRule) {
        rule.onAllNodesWithTag("locale.title", useUnmergedTree = true)
            .filterToOne(
                hasText(language) and hasAnyAncestor(
                    hasAnyDescendant(
                        hasTestTag(
                            if (selected) "locale.selected" else "locale.unselected",
                        ),
                    ),
                ),
            )
            .assertIsDisplayed()
    }

    fun verifyLanguageHeaderIsTranslated(translation: String) =
        assertTrue(mDevice.findObject(UiSelector().text(translation)).waitForExists(waitingTime))

    class Transition {

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.waitForIdle()
            goBackButton().perform(ViewActions.click())

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun goBackButton() =
    onView(CoreMatchers.allOf(ViewMatchers.withContentDescription("Navigate up")))

private fun languagesList(rule: ComposeTestRule) = rule.onNodeWithTag("locale.list")
