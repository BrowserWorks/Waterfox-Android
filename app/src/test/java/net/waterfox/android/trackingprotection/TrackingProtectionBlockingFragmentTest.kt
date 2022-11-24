/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.trackingprotection

import android.content.Context
import androidx.fragment.app.FragmentActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.R
import net.waterfox.android.ext.settings
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.trackingprotection.TrackingProtectionMode.CUSTOM
import org.robolectric.Robolectric

@RunWith(WaterfoxRobolectricTestRunner::class)
class TrackingProtectionBlockingFragmentTest {
    @Test
    fun `GIVEN total cookie protection is enabled WHEN showing details about the protection options THEN show update details for tracking protection`() {
        val expectedTitle = testContext.getString(R.string.etp_cookies_title_2)
        val expectedDescription = testContext.getString(R.string.etp_cookies_description_2)

        mockkStatic("net.waterfox.android.ext.ContextKt") {
            every { any<Context>().settings() } returns mockk(relaxed = true)

            val fragment = createFragment()

            val cookiesCategory = fragment.binding.categoryCookies
            assertEquals(expectedTitle, cookiesCategory.trackingProtectionCategoryTitle.text)
            assertEquals(expectedDescription, cookiesCategory.trackingProtectionCategoryItemDescription.text)
        }
    }

    private fun createFragment(): TrackingProtectionBlockingFragment {
        // Create and attach the fragment ourself instead of using "createAddedTestFragment"
        // to prevent having "onResume -> showToolbar" called.

        val activity = Robolectric.buildActivity(FragmentActivity::class.java)
            .create()
            .start()
            .get()
        val fragment = TrackingProtectionBlockingFragment().apply {
            arguments = TrackingProtectionBlockingFragmentArgs(
                protectionMode = CUSTOM
            ).toBundle()
        }
        activity.supportFragmentManager.beginTransaction()
            .add(fragment, "test")
            .commitNow()

        return fragment
    }
}
