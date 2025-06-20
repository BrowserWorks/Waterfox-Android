/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION")

package net.waterfox.android.ui.robots

import android.util.Log
import android.widget.RelativeLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import net.waterfox.android.R
import net.waterfox.android.helpers.Constants.RETRY_COUNT
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.TestAssetHelper.waitingTimeLong
import net.waterfox.android.helpers.TestHelper.appName
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.TestHelper.restartApp
import net.waterfox.android.helpers.TestHelper.scrollToElementByText
import net.waterfox.android.helpers.click
import net.waterfox.android.helpers.ext.waitNotNull

/**
 * Implementation of Robot Pattern for the Addons Management Settings.
 */

class SettingsSubMenuAddonsManagerRobot {
    fun verifyAddonPermissionPrompt(addonName: String) = assertAddonPermissionPrompt(addonName)

    fun clickInstallAddon(addonName: String) {
        mDevice.waitNotNull(
            Until.findObject(By.textContains(addonName)),
            waitingTime
        )

        installButtonForAddon(addonName)
            .check(matches(isCompletelyDisplayed()))
            .perform(click())
    }

    fun verifyAddonInstallCompleted(addonName: String, activityTestRule: HomeActivityIntentTestRule) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertFalse(
                    mDevice.findObject(UiSelector().text("Failed to install $addonName"))
                        .waitForExists(waitingTime)
                )

                assertTrue(
                    mDevice.findObject(UiSelector().text("Okay, Got it"))
                        .waitForExists(waitingTimeLong)
                )
                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    Log.e("TestLog", "Addon failed to install on try #$i")
                    restartApp(activityTestRule)
                    installAddon(addonName)
                }
            }
        }
    }

    fun verifyAddonInstallCompletedPrompt(addonName: String) {
        onView(
            allOf(
                withText("Okay, Got it"),
                withParent(instanceOf(RelativeLayout::class.java)),
                hasSibling(withText("$addonName has been added to $appName")),
                hasSibling(withText("Open it in the menu")),
                hasSibling(withText("Allow in private browsing"))
            )
        )
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    fun closeAddonInstallCompletePrompt() {
        onView(withText("Okay, Got it")).click()
    }

    fun verifyAddonIsInstalled(addonName: String) {
        scrollToElementByText(addonName)
        assertAddonIsInstalled(addonName)
    }

    fun verifyEnabledTitleDisplayed() {
        onView(withText("Enabled"))
            .check(matches(isCompletelyDisplayed()))
    }

    fun cancelInstallAddon() = cancelInstall()
    fun acceptPermissionToInstallAddon() = allowPermissionToInstall()
    fun verifyAddonsItems() = assertAddonsItems()
    fun verifyAddonCanBeInstalled(addonName: String) = assertAddonCanBeInstalled(addonName)

    fun selectAllowInPrivateBrowsing() {
        assertTrue(
            "Addon install confirmation prompt not displayed",
            mDevice.findObject(UiSelector().text("Allow in private browsing"))
                .waitForExists(waitingTimeLong)
        )
        onView(withId(mozilla.components.feature.addons.R.id.allow_in_private_browsing)).click()
    }

    fun installAddon(addonName: String) {
        homeScreen {
        }.openThreeDotMenu {
        }.openAddonsManagerMenu {
            clickInstallAddon(addonName)
            verifyAddonPermissionPrompt(addonName)
            acceptPermissionToInstallAddon()
        }
    }

    class Transition {
        fun goBack(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            fun goBackButton() = onView(allOf(withContentDescription("Navigate up")))
            goBackButton().click()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun openDetailedMenuForAddon(
            addonName: String,
            interact: SettingsSubMenuAddonsManagerAddonDetailedMenuRobot.() -> Unit
        ): SettingsSubMenuAddonsManagerAddonDetailedMenuRobot.Transition {
            scrollToElementByText(addonName)

            onView(
                allOf(
                    withId(mozilla.components.feature.addons.R.id.add_on_item),
                    hasDescendant(
                        allOf(
                            withId(mozilla.components.feature.addons.R.id.add_on_name),
                            withText(addonName)
                        )
                    )
                )
            ).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
                .perform(click())

            SettingsSubMenuAddonsManagerAddonDetailedMenuRobot().interact()
            return SettingsSubMenuAddonsManagerAddonDetailedMenuRobot.Transition()
        }
    }

    private fun installButtonForAddon(addonName: String) =
        onView(
            allOf(
                withContentDescription(mozilla.components.feature.addons.R.string.mozac_feature_addons_install_addon_content_description_2),
                isDescendantOfA(withId(mozilla.components.feature.addons.R.id.add_on_item)),
                hasSibling(hasDescendant(withText(addonName)))
            )
        )

    private fun assertAddonIsEnabled(addonName: String) {
        installButtonForAddon(addonName)
            .check(matches(not(isCompletelyDisplayed())))
    }

    private fun assertAddonPermissionPrompt(addonName: String) {
        onView(allOf(withId(R.id.title), withText("Add $addonName?")))
            .check(matches(isCompletelyDisplayed()))

        onView(
            allOf(
                withId(R.id.permissions),
                withText(containsString("It requires your permission to:"))
            )
        )
            .check(matches(isCompletelyDisplayed()))

        onView(allOf(withId(mozilla.components.feature.sitepermissions.R.id.allow_button), withText("Add")))
            .check(matches(isCompletelyDisplayed()))

        onView(allOf(withId(mozilla.components.feature.downloads.R.id.deny_button), withText("Cancel")))
            .check(matches(isCompletelyDisplayed()))
    }

    private fun assertAddonIsInstalled(addonName: String) {
        onView(
            allOf(
                withId(R.id.add_button),
                isDescendantOfA(withId(mozilla.components.feature.addons.R.id.add_on_item)),
                hasSibling(hasDescendant(withText(addonName)))
            )
        ).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    private fun cancelInstall() {
        onView(allOf(withId(mozilla.components.feature.downloads.R.id.deny_button), withText("Cancel")))
            .check(matches(isCompletelyDisplayed()))
            .perform(click())
    }

    private fun allowPermissionToInstall() {
        onView(allOf(withId(mozilla.components.feature.sitepermissions.R.id.allow_button), withText("Add")))
            .check(matches(isCompletelyDisplayed()))
            .perform(click())
    }

    private fun assertAddonsItems() {
        assertRecommendedTitleDisplayed()
        assertAddons()
    }

    private fun assertRecommendedTitleDisplayed() {
        onView(allOf(withId(R.id.title), withText("Recommended")))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    private fun assertAddons() {
        assertAddonUblock()
    }

    private fun assertAddonUblock() {
        onView(
            allOf(
                isAssignableFrom(RelativeLayout::class.java),
                withId(mozilla.components.feature.addons.R.id.add_on_item),
                hasDescendant(allOf(withId(mozilla.components.feature.addons.R.id.add_on_icon), isCompletelyDisplayed())),
                hasDescendant(
                    allOf(
                        withId(mozilla.components.feature.addons.R.id.details_container),
                        hasDescendant(withText("uBlock Origin")),
                        hasDescendant(withText("Finally, an efficient wide-spectrum content blocker. Easy on CPU and memory.")),
                        hasDescendant(withId(R.id.rating)),
                        hasDescendant(withId(R.id.review_count))
                    )
                ),
                hasDescendant(withId(R.id.add_button))
            )
        ).check(matches(isCompletelyDisplayed()))
    }

    private fun assertAddonCanBeInstalled(addonName: String) {
        scrollToElementByText(addonName)
        mDevice.waitNotNull(Until.findObject(By.text(addonName)), waitingTime)

        onView(
            allOf(
                withId(R.id.add_button),
                hasSibling(
                    hasDescendant(
                        allOf(
                            withId(mozilla.components.feature.addons.R.id.add_on_name),
                            withText(addonName)
                        )
                    )
                )
            )
        ).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }
}

fun addonsMenu(interact: SettingsSubMenuAddonsManagerRobot.() -> Unit): SettingsSubMenuAddonsManagerRobot.Transition {
    SettingsSubMenuAddonsManagerRobot().interact()
    return SettingsSubMenuAddonsManagerRobot.Transition()
}
