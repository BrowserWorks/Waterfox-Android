/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package net.waterfox.android.ui.robots

import android.os.Build
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.core.content.pm.PackageInfoCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertTrue
import net.waterfox.android.BuildConfig
import net.waterfox.android.R
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.TestHelper
import net.waterfox.android.helpers.TestHelper.appName
import net.waterfox.android.helpers.TestHelper.getStringResource
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.TestHelper.packageName
import net.waterfox.android.settings.SupportUtils
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Date

/**
 * Implementation of Robot Pattern for the settings about sub menu.
 */
class SettingsSubMenuAboutRobot {

    fun verifyAboutWaterfoxPreview(rule: ComposeTestRule) = assertWaterfoxPreviewPage(rule)

    class Transition {
        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            goBackButton().perform(click())

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun assertWaterfoxPreviewPage(rule: ComposeTestRule) {
    assertAboutToolbar()
    assertProductCompany(rule)
    assertVersionNumber(rule)
    assertCurrentTimestamp(rule)
    verifyListElements(rule)
}

private fun assertAboutToolbar() =
    onView(
        allOf(
            withId(R.id.navigationToolbar),
            hasDescendant(withText("About $appName"))
        )
    ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertProductCompany(rule: ComposeTestRule) {
    rule.onNodeWithText("$appName is made by BrowserWorks.")
        .assertIsDisplayed()
}

private fun assertVersionNumber(rule: ComposeTestRule) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo).toString()

    val buildVersion = "${packageInfo.versionName} (Build #$versionCode)\n"
    val componentsVersion =
        "${mozilla.components.Build.version}, ${mozilla.components.Build.gitHash}"
    val geckoVersion =
        org.mozilla.geckoview.BuildConfig.MOZ_APP_VERSION + "-" + org.mozilla.geckoview.BuildConfig.MOZ_APP_BUILDID
    val asVersion = mozilla.components.Build.applicationServicesVersion

    rule.onNodeWithText(buildVersion, substring = true).assertIsDisplayed()
    rule.onNodeWithText(componentsVersion, substring = true).assertIsDisplayed()
    rule.onNodeWithText(geckoVersion, substring = true).assertIsDisplayed()
    rule.onNodeWithText(asVersion, substring = true).assertIsDisplayed()
}

private fun assertCurrentTimestamp(rule: ComposeTestRule) {
    rule
        // When running tests against debug builds, they display a hard-coded string 'debug build'
        // instead of the date. See https://github.com/mozilla-mobile/fenix/pull/10812#issuecomment-633746833
        // .onNodeWithText("debug build").assertIsDisplayed()
        // This assertion should be valid for non-debug build types.
        .onNodeWithTag("about.build.date").assertDisplayedDate()
}

private fun verifyListElements(rule: ComposeTestRule) {
    assertSupport(rule)
    navigateBackToAboutPage(::assertCrashes, rule)
    navigateBackToAboutPage(::assertPrivacyNotice, rule)
    navigateBackToAboutPage(::assertKnowYourRights, rule)
    navigateBackToAboutPage(::assertLicensingInformation, rule)
    navigateBackToAboutPage(::assertLibrariesUsed, rule)
}

private fun assertSupport(rule: ComposeTestRule) {
    val supportLabel = getStringResource(R.string.about_support)
    aboutMenuList(rule).performScrollToNode(hasText(supportLabel))

    rule.onNodeWithText(supportLabel)
        .assertIsDisplayed()
        .performClick()

    TestHelper.verifyUrl(
        "waterfox.net/docs/support",
        "$packageName:id/mozac_browser_toolbar_url_view",
        mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_url_view
    )
}

private fun assertCrashes(rule: ComposeTestRule) {
    val crashesLabel = getStringResource(R.string.about_crashes)
    aboutMenuList(rule).performScrollToNode(hasText(crashesLabel))

    rule.onNodeWithText(crashesLabel)
        .assertIsDisplayed()
        .performClick()

    assertTrue(
        mDevice.findObject(
            UiSelector().textContains("No crash reports have been submitted.")
        ).waitForExists(waitingTime)
    )

    repeat(3) {
        Espresso.pressBack()
    }
}

private fun assertPrivacyNotice(rule: ComposeTestRule) {
    val privacyNoticeLabel = getStringResource(R.string.about_privacy_notice)
    aboutMenuList(rule).performScrollToNode(hasText(privacyNoticeLabel))

    rule.onNodeWithText(privacyNoticeLabel)
        .assertIsDisplayed()
        .performClick()

    TestHelper.verifyUrl(
        "waterfox.net/docs/policies/privacy",
        "$packageName:id/mozac_browser_toolbar_url_view",
        mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_url_view
    )
}

private fun assertKnowYourRights(rule: ComposeTestRule) {
    val knowYourRightsLabel = getStringResource(R.string.about_know_your_rights)
    aboutMenuList(rule).performScrollToNode(hasText(knowYourRightsLabel))

    rule.onNodeWithText(knowYourRightsLabel)
        .assertIsDisplayed()
        .performClick()

    TestHelper.verifyUrl(
        SupportUtils.SumoTopic.YOUR_RIGHTS.topicStr,
        "$packageName:id/mozac_browser_toolbar_url_view",
        mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_url_view
    )
}

private fun assertLicensingInformation(rule: ComposeTestRule) {
    val licensingInformationLabel = getStringResource(R.string.about_licensing_information)
    aboutMenuList(rule).performScrollToNode(hasText(licensingInformationLabel))

    rule.onNodeWithText(licensingInformationLabel)
        .assertIsDisplayed()
        .performClick()

    TestHelper.verifyUrl(
        "about:license",
        "$packageName:id/mozac_browser_toolbar_url_view",
        mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_url_view
    )
}

private fun assertLibrariesUsed(rule: ComposeTestRule) {
    val librariesThatWeUseLabel = getStringResource(R.string.about_other_open_source_libraries)
    aboutMenuList(rule).performScrollToNode(hasText(librariesThatWeUseLabel))

    rule.onNodeWithText(librariesThatWeUseLabel)
        .assertIsDisplayed()
        .performClick()

    onView(withId(R.id.navigationToolbar)).check(matches(hasDescendant(withText(containsString("$appName | OSS Libraries")))))

    rule.onNodeWithTag("about.library.list")
        .performScrollToNode(hasText("org.mozilla.geckoview:geckoview", substring = true))
        .performClick()

    rule.onNode(isDialog()).assertExists()
    // TODO: [Waterfox] fix this, "MPL" substring should be present in license URL
//    rule.onNodeWithText("MPL", substring = true).assertIsDisplayed()
    Espresso.pressBack()
    rule.onNode(isDialog()).assertDoesNotExist()
}

private fun navigateBackToAboutPage(
    itemToInteract: (ComposeTestRule) -> Unit,
    rule: ComposeTestRule
) {
    navigationToolbar {
    }.openThreeDotMenu {
    }.openSettings {
    }.openAboutWaterfoxPreview {
        itemToInteract(rule)
    }
}

private fun aboutMenuList(rule: ComposeTestRule) = rule.onNodeWithTag("about.list")

private fun goBackButton() =
    onView(withContentDescription("Navigate up"))

fun SemanticsNodeInteraction.assertDisplayedDate(): SemanticsNodeInteraction =
    assert(isDisplayedDateAccurate())

private fun isDisplayedDateAccurate(): SemanticsMatcher = SemanticsMatcher(
    "${SemanticsProperties.Text.name} is within range"
) {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    it.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        ?.action
        ?.invoke(textLayoutResults)
    return@SemanticsMatcher if (textLayoutResults.isEmpty()) {
        false
    } else {
        BuildDateAssertion.isDisplayedDateAccurate(
            textLayoutResults.first<TextLayoutResult>().layoutInput.text.text
        )
    }
}

class BuildDateAssertion {
    // When the app is built on firebase, there are times where the BuildDate is off by a few seconds or a few minutes.
    // To compensate for that slight discrepancy, this assertion was added to see if the Build Date shown
    // is within a reasonable amount of time from when the app was built.
    companion object {
        // this pattern represents the following date format: "2023-02-08T13:15:59.431324"
        private const val DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"

        private const val NUM_OF_HOURS = 1

        fun isDisplayedDateAccurate(date: String) = verifyDateIsWithinRange(date, NUM_OF_HOURS)

        private fun verifyDateIsWithinRange(dateText: String, hours: Int): Boolean {
            // This assertion checks whether has defined a range of time
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                val simpleDateFormat = SimpleDateFormat(DATE_PATTERN)
                val date = simpleDateFormat.parse(dateText)
                if (date == null || !date.isWithinRangeOf(hours)) {
                    throw AssertionError("The build date is not within Range.")
                }
            } else {
                val textviewDate = getLocalDateTimeFromString(dateText)
                val buildConfigDate = getLocalDateTimeFromString(BuildConfig.BUILD_DATE)

                if (!buildConfigDate.isEqual(textviewDate) &&
                    !textviewDate.isWithinRangeOf(hours, buildConfigDate)
                ) {
                    throw AssertionError("$textviewDate is not equal to the date within the build config: $buildConfigDate, and are not within a reasonable amount of time from each other.")
                }
            }
            return true
        }

        private fun Date.isWithinRangeOf(hours: Int): Boolean {
            // To determine the date range, the maxDate is retrieved by adding the variable hours to the calendar.
            // Since the calendar will represent the maxDate at this time, to retrieve the minDate the variable hours is multipled by negative 2 and added to the calendar
            // This will result in the maxDate being equal to the original Date + hours, and minDate being equal to original Date - hours

            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            calendar.time = this
            calendar.set(Calendar.YEAR, currentYear)
            val updatedDate = calendar.time

            calendar.add(Calendar.HOUR_OF_DAY, hours)
            val maxDate = calendar.time
            calendar.add(
                Calendar.HOUR_OF_DAY,
                hours * -2
            ) // Gets the minDate by subtracting from maxDate
            val minDate = calendar.time
            return updatedDate.after(minDate) && updatedDate.before(maxDate)
        }

        private fun LocalDateTime.isWithinRangeOf(
            hours: Int,
            baselineDate: LocalDateTime
        ): Boolean {
            val upperBound = baselineDate.plusHours(hours.toLong())
            val lowerBound = baselineDate.minusHours(hours.toLong())
            val currentDate = this
            return currentDate.isAfter(lowerBound) && currentDate.isBefore(upperBound)
        }

        private fun getLocalDateTimeFromString(buildDate: String): LocalDateTime {
            val dateFormatter = DateTimeFormatterBuilder().appendPattern(DATE_PATTERN)
                .parseDefaulting(ChronoField.YEAR, LocalDateTime.now().year.toLong())
                .toFormatter()
            return LocalDateTime.parse(buildDate, dateFormatter)
        }
    }
}
