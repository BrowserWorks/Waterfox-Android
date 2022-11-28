/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings

import androidx.preference.Preference
import io.mockk.mockkStatic
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.R
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner

@RunWith(WaterfoxRobolectricTestRunner::class)
class CustomEtpCookiesOptionsDropDownListPreferenceTest {
    @Test
    fun `GIVEN total cookie protection is enabled WHEN using this preference THEN show the total cookie protection option`() {
        val expectedEntries = arrayOf(
            testContext.getString(R.string.preference_enhanced_tracking_protection_custom_cookies_5)
        ) + defaultEntries
        val expectedValues = arrayOf(testContext.getString(R.string.total_protection)) + defaultValues

        mockkStatic("net.waterfox.android.ext.ContextKt") {
            val preference = CustomEtpCookiesOptionsDropDownListPreference(testContext)

            assertArrayEquals(expectedEntries, preference.entries)
            assertArrayEquals(expectedValues, preference.entryValues)
            assertEquals(expectedValues[0], preference.getDefaultValue())
        }
    }

    /**
     * Use reflection to get the private member holding the default value set for this preference.
     */
    private fun CustomEtpCookiesOptionsDropDownListPreference.getDefaultValue(): String {
        return Preference::class.java
            .getDeclaredField("mDefaultValue").let { field ->
                field.isAccessible = true
                return@let field.get(this) as String
            }
    }

    private val defaultEntries = with(testContext) {
        arrayOf(
            getString(R.string.preference_enhanced_tracking_protection_custom_cookies_1),
            getString(R.string.preference_enhanced_tracking_protection_custom_cookies_2),
            getString(R.string.preference_enhanced_tracking_protection_custom_cookies_3),
            getString(R.string.preference_enhanced_tracking_protection_custom_cookies_4),
        )
    }

    private val defaultValues = with(testContext) {
        arrayOf(
            getString(R.string.social),
            getString(R.string.unvisited),
            getString(R.string.third_party),
            getString(R.string.all),
        )
    }
}
