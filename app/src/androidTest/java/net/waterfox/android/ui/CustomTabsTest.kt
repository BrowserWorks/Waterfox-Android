@file:Suppress("DEPRECATION")

package net.waterfox.android.ui

import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.IntentReceiverActivity
import net.waterfox.android.customannotations.SmokeTest
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.FeatureSettingsHelper
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.helpers.TestAssetHelper
import net.waterfox.android.helpers.TestHelper.createCustomTabIntent
import net.waterfox.android.helpers.TestHelper.openAppFromExternalLink
import net.waterfox.android.ui.robots.browserScreen
import net.waterfox.android.ui.robots.customTabScreen
import net.waterfox.android.ui.robots.navigationToolbar
import net.waterfox.android.ui.robots.notificationShade
import net.waterfox.android.ui.robots.openEditURLView
import net.waterfox.android.ui.robots.searchScreen

class CustomTabsTest {
    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer
    private val customMenuItem = "TestMenuItem"
    /* Updated externalLinks.html to v2.0,
       changed the hypertext reference to mozilla-mobile.github.io/testapp/downloads for "External link"
     */
    private val externalLinksPWAPage = "https://mozilla-mobile.github.io/testapp/v2.0/externalLinks.html"
    private val loginPage = "https://mozilla-mobile.github.io/testapp/loginForm"

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule()

    @get: Rule
    val intentReceiverActivityTestRule = ActivityTestRule(
        IntentReceiverActivity::class.java, true, false
    )

    private val featureSettingsHelper = FeatureSettingsHelper()

    @Before
    fun setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }

        featureSettingsHelper.setTCPCFREnabled(false)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @SmokeTest
    @Test
    fun customTabsOpenExternalLinkTest() {
        val externalLinkURL = "https://mozilla-mobile.github.io/testapp/downloads"

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                externalLinksPWAPage.toUri().toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
            clickLinkMatchingText("External link")
            waitForPageToLoad()
            verifyCustomTabToolbarTitle(externalLinkURL)
        }
    }

    @SmokeTest
    @Test
    fun customTabsSaveLoginTest() {

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                loginPage.toUri().toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
            fillAndSubmitLoginCredentials("mozilla", "waterfox")
        }

        browserScreen {
            verifySaveLoginPromptIsDisplayed()
            saveLoginFromPrompt("Save")
        }

        openAppFromExternalLink(loginPage)

        browserScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins {
            verifySecurityPromptForLogins()
            tapSetupLater()
            verifySavedLoginFromPrompt("mozilla")
        }
    }

    @SmokeTest
    @Test
    fun customTabCopyToolbarUrlTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
                customMenuItem
            )
        )

        customTabScreen {
            longCLickAndCopyToolbarUrl()
        }

        openAppFromExternalLink(customTabPage.url.toString())

        navigationToolbar {
            openEditURLView()
        }

        searchScreen {
            clickClearButton()
            longClickToolbar()
            clickPasteText()
            verifyPastedToolbarText(customTabPage.url.toString())
        }
    }

    @SmokeTest
    @Test
    fun customTabShareTextTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
        }

        browserScreen {
            longClickMatchingText("content")
        }.clickShareSelectedText {
            verifyAndroidShareLayout()
        }
    }

    @SmokeTest
    @Test
    fun customTabDownloadTest() {
        val customTabPage = "https://storage.googleapis.com/mobile_test_assets/test_app/downloads.html"
        val downloadFile = "web_icon.png"

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.toUri().toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
        }

        browserScreen {
        }.clickDownloadLink(downloadFile) {
            verifyDownloadPrompt(downloadFile)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }
        mDevice.openNotification()
        notificationShade {
            verifySystemNotificationExists("Download completed")
        }
    }
}
