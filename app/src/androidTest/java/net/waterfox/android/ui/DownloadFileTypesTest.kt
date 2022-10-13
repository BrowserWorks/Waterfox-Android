/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.core.net.toUri
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import net.waterfox.android.customannotations.SmokeTest
import net.waterfox.android.helpers.FeatureSettingsHelper
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.ui.robots.navigationToolbar

/**
 *  Test for verifying downloading a list of different file types:
 *  - Initiates a download
 *  - Verifies download prompt
 *  - Verifies downloading of varying file types and the appearance inside the Downloads listing.
 **/
@RunWith(Parameterized::class)
class DownloadFileTypesTest(fileName: String) {
    /* Remote test page managed by Mozilla Mobile QA team at https://github.com/mozilla-mobile/testapp */
    private val downloadTestPage = "https://storage.googleapis.com/mobile_test_assets/test_app/downloads.html"
    private var downloadFile: String = fileName
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule()

    companion object {
        // Creating test data. The test will take each file name as a parameter and run it individually.
        @JvmStatic
        @Parameterized.Parameters
        fun downloadList() = listOf(
            "washington.pdf",
            "MyDocument.docx",
            "audioSample.mp3",
            "textfile.txt",
            "web_icon.png",
            "videoSample.webm",
            "CSVfile.csv",
            "XMLfile.xml"
        )
    }

    @Before
    fun setUp() {
        // disabling the jump-back-in pop-up that interferes with the tests.
        featureSettingsHelper.setJumpBackCFREnabled(false)
        // disabling the PWA CFR on 3rd visit
        featureSettingsHelper.disablePwaCFR(true)
    }

    @After
    fun tearDown() {
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @SmokeTest
    @Test
    fun downloadMultipleFileTypesTest() {
        navigationToolbar {
        }.enterURLAndEnterToBrowser(downloadTestPage.toUri()) {
        }.clickDownloadLink(downloadFile) {
            verifyDownloadPrompt(downloadFile)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }.closePrompt {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName(downloadFile)
            verifyDownloadedFileIcon()
        }.exitDownloadsManagerToBrowser { }
    }
}
