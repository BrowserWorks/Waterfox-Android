/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings

import android.content.Context
import android.util.AttributeSet
import net.waterfox.android.R

/**
 * Custom [DropDownListPreference] that automatically builds the list of available options for the
 * custom Enhanced Tracking Protection option depending on the current settings.
 */
class CustomEtpCookiesOptionsDropDownListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : DropDownListPreference(context, attrs) {
    init {
        with(context) {
            entries = arrayOf(
                getString(R.string.preference_enhanced_tracking_protection_custom_cookies_1),
                getString(R.string.preference_enhanced_tracking_protection_custom_cookies_2),
                getString(R.string.preference_enhanced_tracking_protection_custom_cookies_3),
                getString(R.string.preference_enhanced_tracking_protection_custom_cookies_4),
            )

            entryValues = arrayOf(
                getString(R.string.social),
                getString(R.string.unvisited),
                getString(R.string.third_party),
                getString(R.string.all),
            )

            @Suppress("UNCHECKED_CAST")
            // The new "Total cookie protection" must be first item.
            entries = arrayOf(getString(R.string.preference_enhanced_tracking_protection_custom_cookies_5)) +
                    entries as Array<String>
            entryValues = arrayOf(getString(R.string.total_protection)) + entryValues as Array<String>
        }

        setDefaultValue(entryValues.first())
    }
}
