/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.trackingprotection

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.R
import net.waterfox.android.ext.components
import net.waterfox.android.ext.settings
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.trackingprotection.TrackingProtectionCategory.CROSS_SITE_TRACKING_COOKIES
import net.waterfox.android.trackingprotection.TrackingProtectionCategory.SOCIAL_MEDIA_TRACKERS

@RunWith(WaterfoxRobolectricTestRunner::class)
class TrackingProtectionPanelViewTest {

    private lateinit var container: ViewGroup
    private lateinit var interactor: TrackingProtectionPanelInteractor
    private lateinit var view: TrackingProtectionPanelView
    private val baseState = TrackingProtectionState(
        tab = null,
        url = "",
        isTrackingProtectionEnabled = false,
        listTrackers = emptyList(),
        mode = TrackingProtectionState.Mode.Normal,
        lastAccessedCategory = ""
    )

    @Before
    fun setup() {
        container = FrameLayout(testContext)
        interactor = mockk(relaxUnitFun = true)
        view = TrackingProtectionPanelView(container, interactor)
    }

    @Test
    fun testNormalModeUi() {
        mockkStatic("net.waterfox.android.ext.ContextKt") {
            every { any<Context>().settings() } returns mockk(relaxed = true)

            view.update(baseState.copy(mode = TrackingProtectionState.Mode.Normal))
            assertFalse(view.binding.detailsMode.isVisible)
            assertTrue(view.binding.normalMode.isVisible)
            assertTrue(view.binding.protectionSettings.isVisible)
            assertFalse(view.binding.notBlockingHeader.isVisible)
            assertFalse(view.binding.blockingHeader.isVisible)
        }
    }

    @Test
    fun testNormalModeUiCookiesWithTotalCookieProtectionEnabled() {
        mockkStatic("net.waterfox.android.ext.ContextKt") {
            every { any<Context>().settings() } returns mockk {}
            val expectedTitle = testContext.getString(R.string.etp_cookies_title_2)

            view.update(baseState.copy(mode = TrackingProtectionState.Mode.Normal))

            assertEquals(expectedTitle, view.binding.crossSiteTracking.text)
            assertEquals(expectedTitle, view.binding.crossSiteTrackingLoaded.text)
        }
    }

    @Test
    fun testPrivateModeUi() {
        view.update(
            baseState.copy(
                mode = TrackingProtectionState.Mode.Details(
                    selectedCategory = TrackingProtectionCategory.TRACKING_CONTENT,
                    categoryBlocked = false
                )
            )
        )
        assertTrue(view.binding.detailsMode.isVisible)
        assertFalse(view.binding.normalMode.isVisible)
        assertEquals(
            testContext.getString(R.string.etp_tracking_content_title),
            view.binding.categoryTitle.text
        )
        assertEquals(
            testContext.getString(R.string.etp_tracking_content_description),
            view.binding.categoryDescription.text
        )
        assertEquals(
            testContext.getString(R.string.enhanced_tracking_protection_allowed),
            view.binding.detailsBlockingHeader.text
        )
    }

    @Test
    fun testPrivateModeUiCookiesWithTotalCookieProtectionEnabled() {
        mockkStatic("net.waterfox.android.ext.ContextKt") {
            every { any<Context>().settings() } returns mockk {}
            val expectedTitle = testContext.getString(R.string.etp_cookies_title_2)
            val expectedDescription = testContext.getString(R.string.etp_cookies_description_2)

            view.update(
                baseState.copy(
                    mode = TrackingProtectionState.Mode.Details(
                        selectedCategory = CROSS_SITE_TRACKING_COOKIES,
                        categoryBlocked = false
                    )
                )
            )

            assertEquals(expectedTitle, view.binding.categoryTitle.text)
            assertEquals(expectedDescription, view.binding.categoryDescription.text)
        }
    }

    @Test
    fun testProtectionSettings() {
        view.binding.protectionSettings.performClick()
        verify { interactor.selectTrackingProtectionSettings() }
    }

    @Test
    fun testExistDetailModed() {
        view.binding.detailsBack.performClick()
        verify { interactor.onExitDetailMode() }
    }

    @Test
    fun testDetailsBack() {
        view.binding.navigateBack.performClick()
        verify { interactor.onBackPressed() }
    }

    @Test
    fun testSocialMediaTrackerClick() {
        every { testContext.components.analytics } returns mockk(relaxed = true)
        view.binding.socialMediaTrackers.performClick()
        verify { interactor.openDetails(SOCIAL_MEDIA_TRACKERS, categoryBlocked = true) }

        view.binding.socialMediaTrackersLoaded.performClick()
        verify { interactor.openDetails(SOCIAL_MEDIA_TRACKERS, categoryBlocked = false) }
    }

    @Test
    fun testCrossSiteTrackerClick() {
        every { testContext.components.analytics } returns mockk(relaxed = true)

        view.binding.crossSiteTracking.performClick()

        verify { interactor.openDetails(CROSS_SITE_TRACKING_COOKIES, categoryBlocked = true) }

        view.binding.crossSiteTrackingLoaded.performClick()
        verify { interactor.openDetails(CROSS_SITE_TRACKING_COOKIES, categoryBlocked = false) }
    }
}
