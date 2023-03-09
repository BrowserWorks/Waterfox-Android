/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.ui.robots.homeScreen
import org.junit.Rule
import org.junit.Test

class SettingsLocaleTest {

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(
        HomeActivityIntentTestRule()
    ) { it.activity }

    @Test
    fun switchLanguageTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openLanguageSubMenu {
            verifyLanguageList(composeTestRule)

            selectLanguage("English (Canada)", composeTestRule)
            verifyLanguageListItem("English (Canada)", selected = true, composeTestRule)
            verifyLanguageListItem("Follow device language", selected = false, composeTestRule)

            selectLanguage("Follow device language", composeTestRule)
            verifyLanguageListItem("English (Canada)", selected = false, composeTestRule)
            verifyLanguageListItem("Follow device language", selected = true, composeTestRule)
        }
    }

}
