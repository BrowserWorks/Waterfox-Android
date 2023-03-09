/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package net.waterfox.android.ui.robots

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers
import org.junit.Assert.assertTrue
import net.waterfox.android.R
import net.waterfox.android.helpers.Constants.PackageName.GOOGLE_APPS_PHOTOS
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.TestAssetHelper.waitingTimeLong
import net.waterfox.android.helpers.TestHelper
import net.waterfox.android.helpers.TestHelper.assertExternalAppOpens
import net.waterfox.android.helpers.TestHelper.packageName
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.click
import net.waterfox.android.helpers.ext.waitNotNull

/**
 * Implementation of Robot Pattern for download UI handling.
 */

class DownloadRobot {

    fun verifyDownloadPrompt(fileName: String) = assertDownloadPrompt(fileName)

    fun verifyDownloadNotificationPopup() = assertDownloadNotificationPopup()

    fun verifyPhotosAppOpens() = assertExternalAppOpens(GOOGLE_APPS_PHOTOS)

    fun verifyDownloadedFileName(fileName: String, rule: ComposeTestRule) =
        rule.onAllNodesWithTag("library.site.item.title", useUnmergedTree = true)
            .filterToOne(hasText(fileName))
            .assertIsDisplayed()

    fun verifyDownloadedFileIcon(rule: ComposeTestRule) = assertDownloadedFileIcon(rule)

    fun verifyEmptyDownloadsList(rule: ComposeTestRule) =
        rule.onNodeWithText("No downloaded files").assertIsDisplayed()

    fun verifyDownloadsList(rule: ComposeTestRule) =
        rule.onNodeWithTag("download.list").assertIsDisplayed()

    fun openDownloadedFile(fileName: String, rule: ComposeTestRule) =
        downloadedFile(fileName, rule)
            .assertIsDisplayed()
            .performClick()

    class Transition {
        fun clickDownload(interact: DownloadRobot.() -> Unit): Transition {
            downloadButton().click()

            DownloadRobot().interact()
            return Transition()
        }

        fun closePrompt(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            closePromptButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickOpen(type: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            openDownloadButton().click()

            // verify open intent is matched with associated data type
            Intents.intended(
                CoreMatchers.allOf(
                    IntentMatchers.hasAction(Intent.ACTION_VIEW),
                    IntentMatchers.hasType(type)
                )
            )

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickAllowPermission(interact: DownloadRobot.() -> Unit): Transition {
            mDevice.waitNotNull(
                Until.findObject(By.res(TestHelper.getPermissionAllowID() + ":id/permission_allow_button")),
                waitingTime
            )

            val allowPermissionButton = mDevice.findObject(By.res(TestHelper.getPermissionAllowID() + ":id/permission_allow_button"))
            allowPermissionButton.click()

            DownloadRobot().interact()
            return Transition()
        }

        fun exitDownloadsManagerToBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            onView(withContentDescription("Navigate up")).click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

fun downloadRobot(interact: DownloadRobot.() -> Unit): DownloadRobot.Transition {
    DownloadRobot().interact()
    return DownloadRobot.Transition()
}

private fun assertDownloadPrompt(fileName: String) {
    var currentTries = 0
    while (currentTries++ < 3) {
        try {
            assertTrue(
                "Download prompt button not visible",
                mDevice.findObject(UiSelector().resourceId("$packageName:id/download_button"))
                    .waitForExists(waitingTimeLong)
            )
            assertTrue(
                "$fileName title doesn't match",
                mDevice.findObject(UiSelector().text(fileName))
                    .waitForExists(waitingTimeLong)
            )

            break
        } catch (e: AssertionError) {
            android.util.Log.e("DOWNLOAD_ROBOT", "Failed to find locator: ${e.localizedMessage}")

            browserScreen {
            }.clickDownloadLink(fileName) {
            }
        }
    }
}

private fun assertDownloadNotificationPopup() {
    assertTrue(
        "Download notification Open button not found",
        mDevice.findObject(UiSelector().text("Open"))
            .waitForExists(waitingTime)
    )
    assertTrue(
        "Download completed notification text doesn't match",
        mDevice.findObject(UiSelector().textContains("Download completed"))
            .waitForExists(waitingTime)
    )
    assertTrue(
        "Downloaded file name not visible",
        mDevice.findObject(UiSelector().resourceId("$packageName:id/download_dialog_filename"))
            .waitForExists(waitingTime)
    )
}

private fun closePromptButton() =
    onView(withContentDescription("Close"))

private fun downloadButton() =
    onView(withText("Download"))
        .inRoot(isDialog())
        .check(matches(isDisplayed()))

private fun openDownloadButton() =
    onView(withId(R.id.download_dialog_action_button))
        .check(matches(isDisplayed()))

private fun downloadedFile(fileName: String, rule: ComposeTestRule) =
    rule.onNodeWithText(fileName)

private fun assertDownloadedFileIcon(rule: ComposeTestRule) =
    rule.onNodeWithTag("library.site.item.favicon", useUnmergedTree = true).assertExists()
