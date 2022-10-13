/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home

import android.view.View
import androidx.annotation.StringRes
import net.waterfox.android.R
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.browser.browsingmode.BrowsingModeManager

/**
 * Sets up the private browsing toggle button on the [HomeFragment].
 */
class PrivateBrowsingButtonView(
    button: View,
    private val browsingModeManager: BrowsingModeManager,
    private val onClick: (BrowsingMode) -> Unit
) : View.OnClickListener {

    init {
        button.contentDescription = button.context.getString(getContentDescription(browsingModeManager.mode))
        button.setOnClickListener(this)
    }

    /**
     * Calls [onClick] with the new [BrowsingMode] and updates the [browsingModeManager].
     */
    override fun onClick(v: View) {
        val invertedMode = BrowsingMode.fromBoolean(!browsingModeManager.mode.isPrivate)
        onClick(invertedMode)

        browsingModeManager.mode = invertedMode
    }

    companion object {

        /**
         * Returns the appropriate content description depending on the browsing mode.
         */
        @StringRes
        private fun getContentDescription(mode: BrowsingMode) = when (mode) {
            BrowsingMode.Normal -> R.string.content_description_private_browsing_button
            BrowsingMode.Private -> R.string.content_description_disable_private_browsing_button
        }
    }
}
