/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.addons

import android.view.View
import net.waterfox.android.components.WaterfoxSnackbar

/**
 * Shows the Waterfox Snackbar in the given view along with the provided text.
 *
 * @param view A [View] used to determine a parent for the [WaterfoxSnackbar].
 * @param text The text to display in the [WaterfoxSnackbar].
 */
internal fun showSnackBar(view: View, text: String, duration: Int = WaterfoxSnackbar.LENGTH_SHORT) {
    WaterfoxSnackbar.make(
        view = view,
        duration = duration,
        isDisplayedWithBrowserToolbar = true
    )
        .setText(text)
        .show()
}
